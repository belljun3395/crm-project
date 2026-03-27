#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Campaign dashboard API seed start'

seed_funnel_sequence() {
  local external_id="$1"
  local campaign_name="$2"
  shift 2

  for event_name in "$@"; do
    create_seed_event "$event_name" "$external_id" "$campaign_name" >/dev/null
  done
}

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

funnel_snapshot() {
  local campaign_id="$1"
  local steps="${2:-signup,open,click}"

  if funnel_response="$(get_json "/campaigns/${campaign_id}/analytics/funnel?steps=$(url_encode "$steps")")"; then
    json_value "$funnel_response" '[.data.stepMetrics[] | "\(.step)=events:\(.eventCount),qualified:\(.qualifiedUserCount),conversion:\(.conversionFromPrevious)"] | join(" | ")'
    return
  fi

  warn "Funnel lookup failed for campaignId=${campaign_id}"
  printf 'N/A\n'
}

segment_comparison_snapshot() {
  local campaign_id="$1"
  local segment_ids="$2"
  local event_name="${3:-purchase}"

  if segment_response="$(get_json "/campaigns/${campaign_id}/analytics/segment-comparison?segmentIds=$(url_encode "$segment_ids")&eventName=$(url_encode "$event_name")")"; then
    json_value "$segment_response" '[.data.segmentMetrics[] | "\(.segmentName)=target:\(.targetUserCount),eventUsers:\(.eventUserCount),eventCount:\(.eventCount),conversion:\(.conversionRate)"] | join(" | ")'
    return
  fi

  warn "Segment comparison lookup failed for campaignId=${campaign_id}"
  printf 'N/A\n'
}

segment_token="dashboard-segment-target-$(seed_suffix)"
user_a_info="$(create_seed_user "$segment_token")"
IFS='|' read -r user_a_id user_a_external_id _ <<<"$user_a_info"
user_b_info="$(create_seed_user 'dashboard-user-b')"
IFS='|' read -r user_b_id user_b_external_id _ <<<"$user_b_info"

segment_info="$(create_seed_segment_for_email 'dashboard-segment' "$segment_token")"
IFS='|' read -r dashboard_segment_id dashboard_segment_name <<<"$segment_info"

campaign_a_info="$(create_seed_campaign 'dashboard-campaign-a' "$dashboard_segment_id")"
IFS='|' read -r campaign_a_id campaign_a_name <<<"$campaign_a_info"
campaign_b_info="$(create_seed_campaign 'dashboard-campaign-b')"
IFS='|' read -r campaign_b_id campaign_b_name <<<"$campaign_b_info"

# Campaign A: UI 기본 퍼널 입력값(signup,open,click)과 segment 비교(purchase)에 맞춘 데이터.
seed_funnel_sequence "$user_a_external_id" "$campaign_a_name" signup open click purchase
seed_funnel_sequence "$user_b_external_id" "$campaign_a_name" signup open purchase
seed_funnel_sequence "$user_a_external_id" "$campaign_a_name" signup open click purchase

# Campaign B: 단계 일부만 도달한 데이터로 비교 케이스 유지.
seed_funnel_sequence "$user_b_external_id" "$campaign_b_name" signup open
seed_funnel_sequence "$user_a_external_id" "$campaign_b_name" signup

IFS='|' read -r metrics_a total_events_a stream_length_a <<<"$(dashboard_snapshot "$campaign_a_id")"
IFS='|' read -r metrics_b total_events_b stream_length_b <<<"$(dashboard_snapshot "$campaign_b_id")"
funnel_a="$(funnel_snapshot "$campaign_a_id" "signup,open,click")"
funnel_b="$(funnel_snapshot "$campaign_b_id" "signup,open,click")"
segment_a="$(segment_comparison_snapshot "$campaign_a_id" "$dashboard_segment_id" "purchase")"

printf 'Users: A(id=%s externalId=%s), B(id=%s externalId=%s)\n' \
  "$user_a_id" "$user_a_external_id" "$user_b_id" "$user_b_external_id"
printf 'Segment: id=%s name=%s token=%s\n' \
  "$dashboard_segment_id" "$dashboard_segment_name" "$segment_token"
printf 'Campaign A: id=%s name=%s metrics=%s totalEvents=%s streamLength=%s\n' \
  "$campaign_a_id" "$campaign_a_name" "$metrics_a" "$total_events_a" "$stream_length_a"
printf 'Campaign B: id=%s name=%s metrics=%s totalEvents=%s streamLength=%s\n' \
  "$campaign_b_id" "$campaign_b_name" "$metrics_b" "$total_events_b" "$stream_length_b"
printf 'Campaign A Funnel: %s\n' "$funnel_a"
printf 'Campaign B Funnel: %s\n' "$funnel_b"
printf 'Campaign A SegmentComparison(purchase): %s\n' "$segment_a"
if [[ "$total_events_a" == "0" && "$stream_length_a" != "0" ]]; then
  printf 'Note: summary metrics are aggregated by scheduled consumer (runs every 60s). Stream/Funnel values are already available.\n'
fi
printf 'Dashboard demo params (Campaign A): funnelSteps=signup,open,click segmentIds=%s eventName=purchase\n' \
  "$dashboard_segment_id"
printf 'Live stream pump example: bash scripts/test-data/pump-campaign-live-stream.sh %s 30 1\n' \
  "$campaign_a_id"
