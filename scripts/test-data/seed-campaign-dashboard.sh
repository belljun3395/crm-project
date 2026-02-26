#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Campaign dashboard API seed start'

seed_user_info="$(create_seed_user 'dashboard-user')"
IFS='|' read -r user_id external_id user_email <<<"$seed_user_info"

campaign_info="$(create_seed_campaign 'dashboard-campaign')"
IFS='|' read -r campaign_id campaign_name <<<"$campaign_info"

for index in 1 2 3 4 5; do
  create_seed_event "dashboard-event-${index}-$(seed_suffix)" "$external_id" "$campaign_name" >/dev/null
done

metrics_count='N/A'
total_events='N/A'
stream_length='N/A'

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

printf 'Seed user: id=%s externalId=%s email=%s\n' "$user_id" "$external_id" "$user_email"
printf 'Campaign: id=%s name=%s\n' "$campaign_id" "$campaign_name"
printf 'Dashboard metrics count: %s\n' "$metrics_count"
printf 'Campaign total events: %s\n' "$total_events"
printf 'Campaign stream length: %s\n' "$stream_length"
