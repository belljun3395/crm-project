#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Segments API seed start'

update_seed_user_attributes() {
  local user_id="$1"
  local external_id="$2"
  local email="$3"
  local name="$4"
  local tier="${5:-BETA}"

  local attributes
  attributes="$(
    "$JQ_BIN" -cn \
      --arg email "$email" \
      --arg name "$name" \
      --arg tier "$tier" \
      '{email:$email,name:$name,tier:$tier}'
  )"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --argjson id "$user_id" \
      --arg externalId "$external_id" \
      --arg userAttributes "$attributes" \
      '{id:$id,externalId:$externalId,userAttributes:$userAttributes}'
  )"

  post_json '/users' "$payload" 'segments-users-update' >/dev/null
}

segment_token="segment-target-$(seed_suffix)"
event_name_for_segment="segment-event-$(seed_suffix)"

user_a_info="$(create_seed_user 'segments-user-a')"
IFS='|' read -r user_a_id user_a_external_id user_a_email <<<"$user_a_info"
user_b_info="$(create_seed_user 'segments-user-b')"
IFS='|' read -r user_b_id user_b_external_id user_b_email <<<"$user_b_info"
user_c_info="$(create_seed_user 'segments-user-c')"
IFS='|' read -r user_c_id user_c_external_id user_c_email <<<"$user_c_info"

update_seed_user_attributes "$user_a_id" "$user_a_external_id" "${segment_token}-a@example.com" "Segments Target A" "VIP"
update_seed_user_attributes "$user_b_id" "$user_b_external_id" "${segment_token}-b@example.com" "Segments Target B" "BETA"
update_seed_user_attributes "$user_c_id" "$user_c_external_id" "outside-${segment_token}@example.com" "Segments Control C" "FREE"

email_segment_info="$(create_seed_segment_for_email 'segment-email-contains' "$segment_token")"
IFS='|' read -r email_segment_id email_segment_name <<<"$email_segment_info"

event_condition_payload="$(
  "$JQ_BIN" -cn \
    --arg name "segment-event-condition-$(seed_suffix)" \
    --arg eventName "$event_name_for_segment" \
    '{
      name:$name,
      description:"Seed segment: users who have event.name == target",
      active:true,
      conditions:[
        {
          field:"event.name",
          operator:"EQ",
          valueType:"STRING",
          value:$eventName
        }
      ]
    }'
)"
event_segment_response="$(post_json '/segments' "$event_condition_payload" 'segments-create-event')"
event_segment_id="$(json_value "$event_segment_response" '.data.segment.id')"
event_segment_name="$(json_value "$event_segment_response" '.data.segment.name')"

in_condition_payload="$(
  "$JQ_BIN" -cn \
    --arg name "segment-user-id-in-$(seed_suffix)" \
    --argjson userAId "$user_a_id" \
    --argjson userBId "$user_b_id" \
    '{
      name:$name,
      description:"Seed segment: user.id IN [A,B]",
      active:true,
      conditions:[
        {
          field:"user.id",
          operator:"IN",
          valueType:"NUMBER",
          value:[$userAId,$userBId]
        }
      ]
    }'
)"
in_segment_response="$(post_json '/segments' "$in_condition_payload" 'segments-create-in')"
in_segment_id="$(json_value "$in_segment_response" '.data.segment.id')"
in_segment_name="$(json_value "$in_segment_response" '.data.segment.name')"

campaign_info="$(create_seed_campaign 'segments-campaign' "$email_segment_id")"
IFS='|' read -r campaign_id campaign_name <<<"$campaign_info"

create_seed_event "$event_name_for_segment" "$user_b_external_id" >/dev/null
create_seed_event "segment-campaign-event-$(seed_suffix)" "$user_a_external_id" "$campaign_name" >/dev/null

segment_list_response="$(get_json '/segments?limit=50')"
email_segment_detail_response="$(get_json "/segments/${email_segment_id}")"

email_segment_users_response="$(get_json "/segments/${email_segment_id}/users")"
email_segment_users_scoped_response="$(get_json "/segments/${email_segment_id}/users?campaignId=${campaign_id}")"
event_segment_users_response="$(get_json "/segments/${event_segment_id}/users")"
in_segment_users_response="$(get_json "/segments/${in_segment_id}/users")"

segment_count="$(json_value "$segment_list_response" '.data | length')"
email_segment_condition_count="$(json_value "$email_segment_detail_response" '.data.conditions | length')"
email_segment_user_count="$(json_value "$email_segment_users_response" '.data | length')"
email_segment_scoped_user_count="$(json_value "$email_segment_users_scoped_response" '.data | length')"
event_segment_user_count="$(json_value "$event_segment_users_response" '.data | length')"
in_segment_user_count="$(json_value "$in_segment_users_response" '.data | length')"

printf 'Users: A(id=%s), B(id=%s), C(id=%s)\n' "$user_a_id" "$user_b_id" "$user_c_id"
printf 'Campaign: id=%s name=%s\n' "$campaign_id" "$campaign_name"
printf 'Created segment(email): id=%s name=%s\n' "$email_segment_id" "$email_segment_name"
printf 'Created segment(event): id=%s name=%s\n' "$event_segment_id" "$event_segment_name"
printf 'Created segment(in): id=%s name=%s\n' "$in_segment_id" "$in_segment_name"
printf 'Segments API list size: %s\n' "$segment_count"
printf 'Email segment condition count: %s\n' "$email_segment_condition_count"
printf 'Email segment matched users(global): %s\n' "$email_segment_user_count"
printf 'Email segment matched users(campaign scope): %s\n' "$email_segment_scoped_user_count"
printf 'Event segment matched users(global): %s\n' "$event_segment_user_count"
printf 'IN segment matched users(global): %s\n' "$in_segment_user_count"
