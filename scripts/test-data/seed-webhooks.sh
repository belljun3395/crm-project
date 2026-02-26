#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Webhooks API seed start'

webhook_info="$(create_seed_webhook 'webhooks-api')"
IFS='|' read -r webhook_id webhook_name <<<"$webhook_info"

updated_name="${webhook_name}-updated"
update_payload="$(
  "$JQ_BIN" -cn \
    --arg name "$updated_name" \
    '{
      name:$name,
      url:"https://example.com/webhook-updated",
      events:["USER_CREATED"],
      active:true
    }'
)"

update_response="$(put_json "/webhooks/${webhook_id}" "$update_payload" 'webhooks-update')"
updated_webhook_name="$(json_value "$update_response" '.data.name')"

list_response="$(get_json '/webhooks')"
detail_response="$(get_json "/webhooks/${webhook_id}")"
delivery_response="$(get_json "/webhooks/${webhook_id}/deliveries?limit=10")"
dead_letter_response="$(get_json "/webhooks/${webhook_id}/dead-letters?limit=10")"

webhook_count="$(json_value "$list_response" '.data | length')"
delivery_count="$(json_value "$delivery_response" '.data | length')"
dead_letter_count="$(json_value "$dead_letter_response" '.data | length')"

printf 'Created webhook id: %s\n' "$webhook_id"
printf 'Updated webhook name: %s\n' "$updated_webhook_name"
printf 'Webhook detail url: %s\n' "$(json_value "$detail_response" '.data.url')"
printf 'Webhook list size: %s\n' "$webhook_count"
printf 'Webhook delivery log size: %s\n' "$delivery_count"
printf 'Webhook dead-letter size: %s\n' "$dead_letter_count"
