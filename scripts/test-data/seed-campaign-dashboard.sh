#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Campaign dashboard API seed start'

dashboard_snapshot() {
  local campaign_id="$1"
  local metrics_count='N/A'
  local total_events='N/A'
  local stream_length='N/A'

  if dashboard_response="$(get_json "/campaigns/${campaign_id}/dashboard?timeWindowUnit=HOUR")"; then
    metrics_count="$(json_value "$dashboard_response" '.data.metrics | length')"
  else
    warn "Dashboard metrics lookup failed for campaignId=${campaign_id}"
  fi

  if summary_response="$(get_json "/campaigns/${campaign_id}/dashboard/summary")"; then
    total_events="$(json_value "$summary_response" '.data.totalEvents')"
  else
    warn "Dashboard summary lookup failed for campaignId=${campaign_id}"
  fi

  if stream_status_response="$(get_json "/campaigns/${campaign_id}/dashboard/stream/status")"; then
    stream_length="$(json_value "$stream_status_response" '.data.streamLength')"
  else
    warn "Dashboard stream status lookup failed for campaignId=${campaign_id}"
  fi

  printf '%s|%s|%s\n' "$metrics_count" "$total_events" "$stream_length"
}

user_a_info="$(create_seed_user 'dashboard-user-a')"
IFS='|' read -r user_a_id user_a_external_id user_a_email <<<"$user_a_info"
user_b_info="$(create_seed_user 'dashboard-user-b')"
IFS='|' read -r user_b_id user_b_external_id user_b_email <<<"$user_b_info"

campaign_a_info="$(create_seed_campaign 'dashboard-campaign-a')"
IFS='|' read -r campaign_a_id campaign_a_name <<<"$campaign_a_info"
campaign_b_info="$(create_seed_campaign 'dashboard-campaign-b')"
IFS='|' read -r campaign_b_id campaign_b_name <<<"$campaign_b_info"

for index in 1 2 3 4 5; do
  create_seed_event "dashboard-a-event-${index}-$(seed_suffix)" "$user_a_external_id" "$campaign_a_name" >/dev/null
done
for index in 1 2 3; do
  create_seed_event "dashboard-b-event-${index}-$(seed_suffix)" "$user_b_external_id" "$campaign_b_name" >/dev/null
done
create_seed_event "dashboard-b-cross-user-$(seed_suffix)" "$user_a_external_id" "$campaign_b_name" >/dev/null

IFS='|' read -r metrics_a total_events_a stream_length_a <<<"$(dashboard_snapshot "$campaign_a_id")"
IFS='|' read -r metrics_b total_events_b stream_length_b <<<"$(dashboard_snapshot "$campaign_b_id")"

printf 'Users: A(id=%s externalId=%s), B(id=%s externalId=%s)\n' \
  "$user_a_id" "$user_a_external_id" "$user_b_id" "$user_b_external_id"
printf 'Campaign A: id=%s name=%s metrics=%s totalEvents=%s streamLength=%s\n' \
  "$campaign_a_id" "$campaign_a_name" "$metrics_a" "$total_events_a" "$stream_length_a"
printf 'Campaign B: id=%s name=%s metrics=%s totalEvents=%s streamLength=%s\n' \
  "$campaign_b_id" "$campaign_b_name" "$metrics_b" "$total_events_b" "$stream_length_b"
