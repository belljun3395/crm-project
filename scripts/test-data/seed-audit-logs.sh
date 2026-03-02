#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Audit logs API seed start'

actor_a_id="seed-auditor-a-$(seed_suffix)"
actor_b_id="seed-auditor-b-$(seed_suffix)"

create_webhook_with_actor() {
  local actor_id="$1"
  local name_prefix="$2"
  local create_payload
  create_payload="$(
    "$JQ_BIN" -cn \
      --arg name "${name_prefix}-$(seed_suffix)" \
      '{
        name:$name,
        url:"https://example.com/audit-webhook",
        events:["USER_CREATED"],
        active:true
      }'
  )"

  local create_response
  create_response="$(
    request_json 'POST' '/webhooks' "$create_payload" \
      -H "Idempotency-Key: $(make_idempotency_key 'audit-webhook-create')" \
      -H "X-Actor-Id: ${actor_id}"
  )"
  json_value "$create_response" '.data.id'
}

webhook_a_id="$(create_webhook_with_actor "$actor_a_id" 'audit-webhook-a')"
webhook_b_id="$(create_webhook_with_actor "$actor_b_id" 'audit-webhook-b')"

update_payload_a="$(
  "$JQ_BIN" -cn \
    '{
      events:["USER_CREATED","EMAIL_SENT"],
      active:true
    }'
)"
update_payload_b="$(
  "$JQ_BIN" -cn \
    '{
      events:["EMAIL_SENT"],
      active:false
    }'
)"

request_json 'PUT' "/webhooks/${webhook_a_id}" "$update_payload_a" \
  -H "Idempotency-Key: $(make_idempotency_key 'audit-webhook-update-a')" \
  -H "X-Actor-Id: ${actor_a_id}" >/dev/null

request_json 'PUT' "/webhooks/${webhook_b_id}" "$update_payload_b" \
  -H "Idempotency-Key: $(make_idempotency_key 'audit-webhook-update-b')" \
  -H "X-Actor-Id: ${actor_b_id}" >/dev/null

request_json 'DELETE' "/webhooks/${webhook_a_id}" '' \
  -H "X-Actor-Id: ${actor_a_id}" >/dev/null

actor_a_logs_response="$(get_json "/audit-logs?limit=50&actorId=${actor_a_id}")"
actor_b_logs_response="$(get_json "/audit-logs?limit=50&actorId=${actor_b_id}")"
all_logs_response="$(get_json '/audit-logs?limit=50')"

actor_a_count="$(json_value "$actor_a_logs_response" '.data | length')"
actor_b_count="$(json_value "$actor_b_logs_response" '.data | length')"
all_count="$(json_value "$all_logs_response" '.data | length')"

printf 'Audit actor A id: %s logs=%s\n' "$actor_a_id" "$actor_a_count"
printf 'Audit actor B id: %s logs=%s\n' "$actor_b_id" "$actor_b_count"
printf 'Audit logs total (limit=50): %s\n' "$all_count"
