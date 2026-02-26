#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Audit logs API seed start'

audit_actor_id="seed-auditor-$(seed_suffix)"

create_payload="$(
  "$JQ_BIN" -cn \
    --arg name "audit-webhook-$(seed_suffix)" \
    '{
      name:$name,
      url:"https://example.com/audit-webhook",
      events:["USER_CREATED"],
      active:true
    }'
)"

create_response="$(
  request_json 'POST' '/webhooks' "$create_payload" \
    -H "Idempotency-Key: $(make_idempotency_key 'audit-webhook-create')" \
    -H "X-Actor-Id: ${audit_actor_id}"
)"

webhook_id="$(json_value "$create_response" '.data.id')"

update_payload="$(
  "$JQ_BIN" -cn \
    '{
      events:["USER_CREATED","EMAIL_SENT"],
      active:true
    }'
)"

request_json 'PUT' "/webhooks/${webhook_id}" "$update_payload" \
  -H "Idempotency-Key: $(make_idempotency_key 'audit-webhook-update')" \
  -H "X-Actor-Id: ${audit_actor_id}" >/dev/null

request_json 'DELETE' "/webhooks/${webhook_id}" '' \
  -H "X-Actor-Id: ${audit_actor_id}" >/dev/null

audit_logs_response="$(get_json "/audit-logs?limit=20&actorId=${audit_actor_id}")"
audit_count="$(json_value "$audit_logs_response" '.data | length')"

printf 'Audit actor id: %s\n' "$audit_actor_id"
printf 'Generated audit log count: %s\n' "$audit_count"
