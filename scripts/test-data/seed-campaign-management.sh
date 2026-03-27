#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Campaign management API seed start'

segment_token="campaign-mgmt-target-$(seed_suffix)"
user_info="$(create_seed_user "$segment_token")"
IFS='|' read -r user_id user_external_id user_email <<<"$user_info"
segment_info="$(create_seed_segment_for_email 'campaign-mgmt-segment' "$segment_token")"
IFS='|' read -r segment_id segment_name <<<"$segment_info"

campaign_name="campaign-mgmt-$(seed_suffix)"
create_payload="$(
  "$JQ_BIN" -cn \
    --arg name "$campaign_name" \
    --argjson segmentId "$segment_id" \
    '{
      name:$name,
      properties:[
        {key:"channel",value:"email"},
        {key:"source",value:"seed-script"},
        {key:"goal",value:"retention"}
      ],
      segmentIds:[$segmentId]
    }'
)"

create_response="$(post_json '/campaigns' "$create_payload" 'campaign-management-create')"
campaign_id="$(json_value "$create_response" '.data.id')"

list_response="$(get_json '/campaigns?limit=100')"
listed_before_delete="$(
  printf '%s\n' "$list_response" | "$JQ_BIN" -r --argjson campaignId "$campaign_id" \
    'any(.data[]; .id == $campaignId)'
)"

detail_response="$(get_json "/campaigns/${campaign_id}")"
detail_name_before="$(json_value "$detail_response" '.data.name')"

updated_campaign_name="${campaign_name}-updated"
update_payload="$(
  "$JQ_BIN" -cn \
    --arg name "$updated_campaign_name" \
    --argjson segmentId "$segment_id" \
    '{
      name:$name,
      properties:[
        {key:"channel",value:"email"},
        {key:"source",value:"seed-script"},
        {key:"goal",value:"conversion"}
      ],
      segmentIds:[$segmentId]
    }'
)"

update_response="$(put_json "/campaigns/${campaign_id}" "$update_payload" 'campaign-management-update')"
detail_name_after_update="$(json_value "$update_response" '.data.name')"

delete_json "/campaigns/${campaign_id}" >/dev/null

list_after_delete_response="$(get_json '/campaigns?limit=100')"
listed_after_delete="$(
  printf '%s\n' "$list_after_delete_response" | "$JQ_BIN" -r --argjson campaignId "$campaign_id" \
    'any(.data[]; .id == $campaignId)'
)"

deleted_get_failed='false'
if get_json "/campaigns/${campaign_id}" >/dev/null; then
  warn "Campaign detail still accessible after delete: campaignId=${campaign_id}"
else
  deleted_get_failed='true'
fi

printf 'Seed user for segment: id=%s externalId=%s email=%s\n' "$user_id" "$user_external_id" "$user_email"
printf 'Segment: id=%s name=%s token=%s\n' "$segment_id" "$segment_name" "$segment_token"
printf 'Campaign lifecycle: createdId=%s nameBefore=%s nameAfter=%s\n' \
  "$campaign_id" "$detail_name_before" "$detail_name_after_update"
printf 'List contains campaign: beforeDelete=%s afterDelete=%s\n' \
  "$listed_before_delete" "$listed_after_delete"
printf 'Deleted campaign detail request fails as expected: %s\n' "$deleted_get_failed"
