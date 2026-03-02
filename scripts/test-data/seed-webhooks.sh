#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Webhooks API seed start'

webhook_a_info="$(create_seed_webhook 'webhooks-api-a')"
IFS='|' read -r webhook_a_id webhook_a_name <<<"$webhook_a_info"

webhook_b_payload="$(
  "$JQ_BIN" -cn \
    --arg name "webhooks-api-b-$(seed_suffix)" \
      '{
      name:$name,
      url:"https://example.com/webhook-b",
      events:["EMAIL_SENT","USER_CREATED"],
      active:false
    }'
)"
webhook_b_response="$(post_json '/webhooks' "$webhook_b_payload" 'webhooks-create-b')"
webhook_b_id="$(json_value "$webhook_b_response" '.data.id')"
webhook_b_name="$(json_value "$webhook_b_response" '.data.name')"

update_a_payload="$(
  "$JQ_BIN" -cn \
    --arg name "${webhook_a_name}-updated" \
    '{
      name:$name,
      url:"https://example.com/webhook-a-updated",
      events:["USER_CREATED","EMAIL_SENT"],
      active:true
    }'
)"
update_b_payload="$(
  "$JQ_BIN" -cn \
    --arg name "${webhook_b_name}-active" \
      '{
      name:$name,
      url:"https://example.com/webhook-b-active",
      events:["USER_CREATED","EMAIL_SENT"],
      active:true
    }'
)"

update_a_response="$(put_json "/webhooks/${webhook_a_id}" "$update_a_payload" 'webhooks-update-a')"
update_b_response="$(put_json "/webhooks/${webhook_b_id}" "$update_b_payload" 'webhooks-update-b')"

list_response="$(get_json '/webhooks')"
detail_a_response="$(get_json "/webhooks/${webhook_a_id}")"
detail_b_response="$(get_json "/webhooks/${webhook_b_id}")"
delivery_a_response="$(get_json "/webhooks/${webhook_a_id}/deliveries?limit=10")"
dead_letter_a_response="$(get_json "/webhooks/${webhook_a_id}/dead-letters?limit=10")"
delivery_b_response="$(get_json "/webhooks/${webhook_b_id}/deliveries?limit=10")"
dead_letter_b_response="$(get_json "/webhooks/${webhook_b_id}/dead-letters?limit=10")"

webhook_count="$(json_value "$list_response" '.data | length')"
active_webhook_count="$(json_value "$list_response" '[.data[] | select(.active == true)] | length')"
delivery_a_count="$(json_value "$delivery_a_response" '.data | length')"
dead_letter_a_count="$(json_value "$dead_letter_a_response" '.data | length')"
delivery_b_count="$(json_value "$delivery_b_response" '.data | length')"
dead_letter_b_count="$(json_value "$dead_letter_b_response" '.data | length')"

printf 'Webhook A: id=%s name=%s updatedName=%s\n' \
  "$webhook_a_id" "$webhook_a_name" "$(json_value "$update_a_response" '.data.name')"
printf 'Webhook B: id=%s name=%s updatedName=%s\n' \
  "$webhook_b_id" "$webhook_b_name" "$(json_value "$update_b_response" '.data.name')"
printf 'Webhook detail A url: %s\n' "$(json_value "$detail_a_response" '.data.url')"
printf 'Webhook detail B url: %s\n' "$(json_value "$detail_b_response" '.data.url')"
printf 'Webhook list size: %s (active=%s)\n' "$webhook_count" "$active_webhook_count"
printf 'Webhook A delivery/dead-letter sizes: %s/%s\n' "$delivery_a_count" "$dead_letter_a_count"
printf 'Webhook B delivery/dead-letter sizes: %s/%s\n' "$delivery_b_count" "$dead_letter_b_count"
