#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Actions API seed start'

campaign_a_info="$(create_seed_campaign 'actions-campaign-a')"
IFS='|' read -r campaign_a_id campaign_a_name <<<"$campaign_a_info"
campaign_b_info="$(create_seed_campaign 'actions-campaign-b')"
IFS='|' read -r campaign_b_id campaign_b_name <<<"$campaign_b_info"

dispatch_payload_a="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_a_id" \
    '{
      channel:"EMAIL",
      destination:"action-seed-a@example.com",
      subject:"[SEED] Action Dispatch A",
      body:"Hello {{name}}, campaign A dispatch.",
      variables:{name:"Operator-A"},
      campaignId:$campaignId
    }'
)"
dispatch_payload_b="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_b_id" \
    '{
      channel:"EMAIL",
      destination:"action-seed-b@example.com",
      subject:"[SEED] Action Dispatch B",
      body:"Hello {{name}}, campaign B dispatch.",
      variables:{name:"Operator-B"},
      campaignId:$campaignId
    }'
)"
dispatch_payload_no_campaign="$(
  "$JQ_BIN" -cn \
    '{
      channel:"EMAIL",
      destination:"action-seed-nocampaign@example.com",
      subject:"[SEED] Action Dispatch No Campaign",
      body:"Hello {{name}}, no campaign dispatch.",
      variables:{name:"Operator-NC"}
    }'
)"

dispatch_response_a="$(post_json '/actions/dispatch' "$dispatch_payload_a" 'actions-dispatch-a')"
dispatch_response_b="$(post_json '/actions/dispatch' "$dispatch_payload_b" 'actions-dispatch-b')"
dispatch_response_no_campaign="$(post_json '/actions/dispatch' "$dispatch_payload_no_campaign" 'actions-dispatch-nocampaign')"

status_a="$(json_value "$dispatch_response_a" '.data.status')"
status_b="$(json_value "$dispatch_response_b" '.data.status')"
status_no_campaign="$(json_value "$dispatch_response_no_campaign" '.data.status')"

histories_a_response="$(get_json "/actions/dispatch/histories?campaignId=${campaign_a_id}")"
histories_b_response="$(get_json "/actions/dispatch/histories?campaignId=${campaign_b_id}")"
history_a_count="$(json_value "$histories_a_response" '.data | length')"
history_b_count="$(json_value "$histories_b_response" '.data | length')"

history_all_count='N/A'
if histories_all_response="$(get_json '/actions/dispatch/histories')"; then
  history_all_count="$(json_value "$histories_all_response" '.data | length')"
else
  warn 'Global actions history lookup failed (without campaign filter).'
fi

printf 'Campaign A: id=%s name=%s\n' "$campaign_a_id" "$campaign_a_name"
printf 'Campaign B: id=%s name=%s\n' "$campaign_b_id" "$campaign_b_name"
printf 'Action dispatch statuses: A=%s B=%s noCampaign=%s\n' \
  "$status_a" "$status_b" "$status_no_campaign"
printf 'Action dispatch history size (campaign A): %s\n' "$history_a_count"
printf 'Action dispatch history size (campaign B): %s\n' "$history_b_count"
printf 'Action dispatch history size (global): %s\n' "$history_all_count"
