#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Events API seed start'

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

  post_json '/users' "$payload" 'events-users-update' >/dev/null
}

primary_user_info="$(create_seed_user 'events-primary-user')"
IFS='|' read -r primary_user_id primary_external_id primary_email <<<"$primary_user_info"
segment_user_a_info="$(create_seed_user 'events-segment-user-a')"
IFS='|' read -r segment_user_a_id segment_user_a_external_id segment_user_a_email <<<"$segment_user_a_info"
segment_user_b_info="$(create_seed_user 'events-segment-user-b')"
IFS='|' read -r segment_user_b_id segment_user_b_external_id segment_user_b_email <<<"$segment_user_b_info"

segment_token="events-segment-$(seed_suffix)"
update_seed_user_attributes "$segment_user_a_id" "$segment_user_a_external_id" "${segment_token}-a@example.com" "Events Segment A"
update_seed_user_attributes "$segment_user_b_id" "$segment_user_b_external_id" "${segment_token}-b@example.com" "Events Segment B"

segment_info="$(create_seed_segment_for_email 'events-segment' "$segment_token")"
IFS='|' read -r segment_id segment_name <<<"$segment_info"

campaign_info="$(create_seed_campaign 'events-campaign' "$segment_id")"
IFS='|' read -r campaign_id campaign_name <<<"$campaign_info"

direct_event_name="event-direct-$(seed_suffix)"
campaign_event_name="event-campaign-$(seed_suffix)"
segment_bulk_event_name="event-segment-bulk-$(seed_suffix)"
custom_property_event_name="event-custom-prop-$(seed_suffix)"

direct_event_id="$(create_seed_event "$direct_event_name" "$primary_external_id")"
campaign_event_id="$(create_seed_event "$campaign_event_name" "$primary_external_id" "$campaign_name")"
segment_bulk_event_id="$(create_seed_event "$segment_bulk_event_name" "$primary_external_id" "$campaign_name" "$segment_id")"

custom_event_payload="$(
  "$JQ_BIN" -cn \
    --arg name "$custom_property_event_name" \
    --arg externalId "$segment_user_a_external_id" \
    --arg campaignName "$campaign_name" \
    '{
      name:$name,
      externalId:$externalId,
      campaignName:$campaignName,
      properties:[
        {key:"channel",value:"email"},
        {key:"source",value:"seed-script"},
        {key:"category",value:"electronics"},
        {key:"price",value:"199.99"}
      ]
    }'
)"
custom_event_response="$(post_json '/events' "$custom_event_payload" 'events-custom')"
custom_event_id="$(json_value "$custom_event_response" '.data.id')"

search_where="source&seed-script&=&END"
encoded_where="$(url_encode "$search_where")"
search_response="$(get_json "/events?eventName=${campaign_event_name}&where=${encoded_where}")"
search_count="$(json_value "$search_response" '.data.events | length')"

custom_where="category&electronics&=&END"
encoded_custom_where="$(url_encode "$custom_where")"
custom_search_response="$(get_json "/events?eventName=${custom_property_event_name}&where=${encoded_custom_where}")"
custom_search_count="$(json_value "$custom_search_response" '.data.events | length')"

all_events_response="$(get_json '/events/all?limit=100')"
all_events_count="$(json_value "$all_events_response" '.data.events | length')"

printf 'Primary user: id=%s externalId=%s email=%s\n' "$primary_user_id" "$primary_external_id" "$primary_email"
printf 'Segment users: A(id=%s) B(id=%s)\n' "$segment_user_a_id" "$segment_user_b_id"
printf 'Segment: id=%s name=%s\n' "$segment_id" "$segment_name"
printf 'Campaign: id=%s name=%s\n' "$campaign_id" "$campaign_name"
printf 'Created event IDs: direct=%s campaign=%s segmentBulk=%s custom=%s\n' \
  "$direct_event_id" "$campaign_event_id" "$segment_bulk_event_id" "$custom_event_id"
printf 'Event search count (source filter, eventName=%s): %s\n' "$campaign_event_name" "$search_count"
printf 'Event search count (custom category filter, eventName=%s): %s\n' "$custom_property_event_name" "$custom_search_count"
printf 'All events count (limit=100): %s\n' "$all_events_count"
