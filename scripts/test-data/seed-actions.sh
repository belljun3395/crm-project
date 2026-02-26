#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Actions API seed start'

campaign_info="$(create_seed_campaign 'actions-campaign')"
IFS='|' read -r campaign_id campaign_name <<<"$campaign_info"

action_payload="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_id" \
    '{
      channel:"EMAIL",
      destination:"action-seed@example.com",
      subject:"[SEED] Action Dispatch",
      body:"Hello {{name}}, this is a seeded action dispatch.",
      variables:{name:"Operator"},
      campaignId:$campaignId
    }'
)"

dispatch_response="$(post_json '/actions/dispatch' "$action_payload" 'actions-dispatch')"
status_value="$(json_value "$dispatch_response" '.data.status')"

histories_response="$(get_json "/actions/dispatch/histories?campaignId=${campaign_id}")"
history_count="$(json_value "$histories_response" '.data | length')"

printf 'Campaign: id=%s name=%s\n' "$campaign_id" "$campaign_name"
printf 'Action dispatch status: %s\n' "$status_value"
printf 'Action dispatch history size (campaign filter): %s\n' "$history_count"
