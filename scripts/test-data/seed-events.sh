#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Events API seed start'

primary_user_info="$(create_seed_user 'events-primary-user')"
IFS='|' read -r primary_user_id primary_external_id primary_email <<<"$primary_user_info"

segment_token="events-segment-$(seed_suffix)"
segment_user_info="$(create_seed_user "$segment_token")"
IFS='|' read -r segment_user_id segment_external_id segment_email <<<"$segment_user_info"

segment_info="$(create_seed_segment_for_email 'events-segment' "$segment_token")"
IFS='|' read -r segment_id segment_name <<<"$segment_info"

campaign_info="$(create_seed_campaign 'events-campaign' "$segment_id")"
IFS='|' read -r campaign_id campaign_name <<<"$campaign_info"

single_event_name="event-single-$(seed_suffix)"
segment_event_name="event-segment-$(seed_suffix)"

single_event_id="$(create_seed_event "$single_event_name" "$primary_external_id" "$campaign_name")"
segment_event_id="$(create_seed_event "$segment_event_name" "$segment_external_id" "$campaign_name" "$segment_id")"

search_where="source&seed-script&=&END"
encoded_where="$(url_encode "$search_where")"
search_response="$(get_json "/events?eventName=${single_event_name}&where=${encoded_where}")"
search_count="$(json_value "$search_response" '.data.events | length')"

printf 'Primary user: id=%s externalId=%s email=%s\n' "$primary_user_id" "$primary_external_id" "$primary_email"
printf 'Segment user: id=%s externalId=%s email=%s\n' "$segment_user_id" "$segment_external_id" "$segment_email"
printf 'Segment: id=%s name=%s\n' "$segment_id" "$segment_name"
printf 'Campaign: id=%s name=%s\n' "$campaign_id" "$campaign_name"
printf 'Created event IDs: single=%s segment=%s\n' "$single_event_id" "$segment_event_id"
printf 'Event search result count for "%s": %s\n' "$single_event_name" "$search_count"
