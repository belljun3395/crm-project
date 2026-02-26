#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
SEED_PREFIX="${SEED_PREFIX:-seed}"
SEED_TIMEOUT_SECONDS="${SEED_TIMEOUT_SECONDS:-20}"
CURL_BIN="${CURL_BIN:-curl}"
JQ_BIN="${JQ_BIN:-jq}"

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  echo "This script is a library and must be sourced." >&2
  exit 1
fi

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is missing: ${command_name}" >&2
    exit 1
  fi
}

init_seed_env() {
  require_command "$CURL_BIN"
  require_command "$JQ_BIN"
}

log() {
  printf '[seed][%s] %s\n' "$(date '+%H:%M:%S')" "$*" >&2
}

warn() {
  printf '[seed][WARN] %s\n' "$*" >&2
}

seed_suffix() {
  printf '%s-%s' "$(date +%s)" "$RANDOM"
}

make_idempotency_key() {
  local scope="${1:-general}"
  printf '%s:%s:%s:%s' "$SEED_PREFIX" "$scope" "$(date +%s)" "$RANDOM"
}

url_encode() {
  local value="$1"
  "$JQ_BIN" -nr --arg value "$value" '$value|@uri'
}

future_time_iso_local() {
  local plus_hours="${1:-1}"

  if date -u -v+"${plus_hours}"H '+%Y-%m-%dT%H:%M:%S' >/dev/null 2>&1; then
    date -u -v+"${plus_hours}"H '+%Y-%m-%dT%H:%M:%S'
    return
  fi

  date -u -d "+${plus_hours} hour" '+%Y-%m-%dT%H:%M:%S'
}

json_value() {
  local json="$1"
  local jq_filter="$2"
  printf '%s\n' "$json" | "$JQ_BIN" -er "$jq_filter"
}

request_json() {
  local method="$1"
  local path="$2"
  local payload="${3:-}"
  shift 3 || true

  local url="${BASE_URL}${path}"
  local -a curl_args=(
    -sS
    --connect-timeout 5
    --max-time "$SEED_TIMEOUT_SECONDS"
    -X "$method"
    -H 'Accept: application/json'
    "$url"
    -w $'\n%{http_code}'
  )

  if [[ -n "$payload" ]]; then
    curl_args+=(-H 'Content-Type: application/json' -d "$payload")
  fi

  if (( $# > 0 )); then
    curl_args+=("$@")
  fi

  local response
  if ! response="$($CURL_BIN "${curl_args[@]}")"; then
    warn "Request failed: ${method} ${path}"
    return 1
  fi

  local status="${response##*$'\n'}"
  local body="${response%$'\n'*}"

  if [[ ! "$status" =~ ^[0-9]{3}$ ]]; then
    warn "Unexpected HTTP status while calling ${method} ${path}: ${status}"
    printf '%s\n' "$response" >&2
    return 1
  fi

  if (( status < 200 || status >= 300 )); then
    warn "HTTP ${status} for ${method} ${path}"
    if [[ -n "$body" ]]; then
      if ! printf '%s\n' "$body" | "$JQ_BIN" . >&2; then
        printf '%s\n' "$body" >&2
      fi
    fi
    return 1
  fi

  printf '%s' "$body"
}

get_json() {
  local path="$1"
  request_json 'GET' "$path" ''
}

delete_json() {
  local path="$1"
  request_json 'DELETE' "$path" ''
}

post_json() {
  local path="$1"
  local payload="$2"
  local scope="${3:-post}"

  request_json 'POST' "$path" "$payload" \
    -H "Idempotency-Key: $(make_idempotency_key "$scope")"
}

put_json() {
  local path="$1"
  local payload="$2"
  local scope="${3:-put}"

  request_json 'PUT' "$path" "$payload" \
    -H "Idempotency-Key: $(make_idempotency_key "$scope")"
}

create_seed_user() {
  local namespace="${1:-user}"
  local suffix
  suffix="$(seed_suffix)"

  local external_id="${namespace}-${suffix}"
  local email="${namespace}-${suffix}@example.com"
  local name="Seed ${namespace} ${suffix}"

  local attributes
  attributes="$(
    "$JQ_BIN" -cn \
      --arg email "$email" \
      --arg name "$name" \
      --arg tier 'BETA' \
      '{email:$email,name:$name,tier:$tier}'
  )"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --arg externalId "$external_id" \
      --arg userAttributes "$attributes" \
      '{externalId:$externalId,userAttributes:$userAttributes}'
  )"

  local response
  response="$(post_json '/users' "$payload" 'users')"

  local user_id
  user_id="$(json_value "$response" '.data.id')"

  printf '%s|%s|%s\n' "$user_id" "$external_id" "$email"
}

create_seed_segment_for_email() {
  local namespace="${1:-segment}"
  local contains_text="$2"
  local suffix
  suffix="$(seed_suffix)"

  local segment_name="${namespace}-${suffix}"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --arg name "$segment_name" \
      --arg containsText "$contains_text" \
      '{
        name:$name,
        description:"Seed segment for API test data",
        active:true,
        conditions:[
          {
            field:"user.email",
            operator:"CONTAINS",
            valueType:"STRING",
            value:$containsText
          }
        ]
      }'
  )"

  local response
  response="$(post_json '/segments' "$payload" 'segments')"

  local segment_id
  segment_id="$(json_value "$response" '.data.segment.id')"

  printf '%s|%s\n' "$segment_id" "$segment_name"
}

create_seed_campaign() {
  local namespace="${1:-campaign}"
  local segment_id="${2:-}"
  local suffix
  suffix="$(seed_suffix)"

  local campaign_name="${namespace}-${suffix}"
  local payload

  if [[ -n "$segment_id" ]]; then
    payload="$(
      "$JQ_BIN" -cn \
        --arg name "$campaign_name" \
        --argjson segmentId "$segment_id" \
        '{
          name:$name,
          properties:[
            {key:"channel",value:"email"},
            {key:"source",value:"seed-script"}
          ],
          segmentIds:[$segmentId]
        }'
    )"
  else
    payload="$(
      "$JQ_BIN" -cn \
        --arg name "$campaign_name" \
        '{
          name:$name,
          properties:[
            {key:"channel",value:"email"},
            {key:"source",value:"seed-script"}
          ]
        }'
    )"
  fi

  local response
  response="$(post_json '/events/campaign' "$payload" 'events-campaign')"

  local campaign_id
  campaign_id="$(json_value "$response" '.data.id')"

  printf '%s|%s\n' "$campaign_id" "$campaign_name"
}

create_seed_event() {
  local event_name="$1"
  local external_id="$2"
  local campaign_name="${3:-}"
  local segment_id="${4:-}"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --arg name "$event_name" \
      --arg campaignName "$campaign_name" \
      --arg externalId "$external_id" \
      --arg segmentId "$segment_id" \
      '
      ({
        name:$name,
        externalId:$externalId,
        properties:[
          {key:"channel",value:"email"},
          {key:"source",value:"seed-script"}
        ]
      }
      + (if ($campaignName | length) > 0 then {campaignName:$campaignName} else {} end)
      + (if ($segmentId | length) > 0 then {segmentId:($segmentId | tonumber)} else {} end))
      '
  )"

  local response
  response="$(post_json '/events' "$payload" 'events')"

  json_value "$response" '.data.id'
}

create_seed_template() {
  local namespace="${1:-template}"
  local suffix
  suffix="$(seed_suffix)"

  local template_name="${namespace}-${suffix}"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --arg templateName "$template_name" \
      --arg subject "[SEED] ${template_name}" \
      --arg body '<p>Hello from seed template.</p>' \
      '{
        templateName:$templateName,
        subject:$subject,
        body:$body,
        variables:[]
      }'
  )"

  local response
  response="$(post_json '/emails/templates' "$payload" 'emails-template')"

  local template_id
  template_id="$(json_value "$response" '.data.id')"

  printf '%s|%s\n' "$template_id" "$template_name"
}

create_seed_webhook() {
  local namespace="${1:-webhook}"
  local suffix
  suffix="$(seed_suffix)"

  local webhook_name="${namespace}-${suffix}"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --arg name "$webhook_name" \
      '{
        name:$name,
        url:"https://example.com/webhook",
        events:["USER_CREATED","EMAIL_SENT"],
        active:true
      }'
  )"

  local response
  response="$(post_json '/webhooks' "$payload" 'webhooks-create')"

  local webhook_id
  webhook_id="$(json_value "$response" '.data.id')"

  printf '%s|%s\n' "$webhook_id" "$webhook_name"
}
