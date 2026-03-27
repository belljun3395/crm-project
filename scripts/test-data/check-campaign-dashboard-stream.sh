#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

campaign_id="${1:-}"
duration_seconds="${2:-8}"
pump_duration_seconds="${3:-3}"
pump_interval_seconds="${4:-1}"

if [[ -z "$campaign_id" ]]; then
  echo "Usage: bash scripts/test-data/check-campaign-dashboard-stream.sh <campaignId> [durationSeconds=8] [pumpDurationSeconds=3] [pumpIntervalSeconds=1]" >&2
  exit 1
fi

if [[ ! "$campaign_id" =~ ^[0-9]+$ ]] || [[ "$campaign_id" -le 0 ]]; then
  echo "campaignId must be a positive integer: $campaign_id" >&2
  exit 1
fi

if [[ ! "$duration_seconds" =~ ^[0-9]+$ ]] || [[ "$duration_seconds" -le 0 ]]; then
  echo "durationSeconds must be a positive integer: $duration_seconds" >&2
  exit 1
fi

if [[ ! "$pump_duration_seconds" =~ ^[0-9]+$ ]] || [[ "$pump_duration_seconds" -le 0 ]]; then
  echo "pumpDurationSeconds must be a positive integer: $pump_duration_seconds" >&2
  exit 1
fi

if [[ ! "$pump_interval_seconds" =~ ^[0-9]+$ ]] || [[ "$pump_interval_seconds" -le 0 ]]; then
  echo "pumpIntervalSeconds must be a positive integer: $pump_interval_seconds" >&2
  exit 1
fi

campaign_response="$(get_json "/campaigns/${campaign_id}")"
campaign_name="$(json_value "$campaign_response" '.data.name')"
stream_url="${BASE_URL}/campaigns/${campaign_id}/dashboard/stream?durationSeconds=3600"

sse_out_file="$(mktemp)"
sse_err_file="$(mktemp)"
pump_log_file="$(mktemp)"

cleanup() {
  rm -f "$sse_out_file" "$sse_err_file" "$pump_log_file"
}
trap cleanup EXIT

log "SSE stream check start (campaignId=${campaign_id}, campaignName=${campaign_name}, duration=${duration_seconds}s)"

(curl -N --max-time "$duration_seconds" "$stream_url" >"$sse_out_file" 2>"$sse_err_file") &
sse_pid=$!

sleep 1
bash "${SCRIPT_DIR}/pump-campaign-live-stream.sh" "$campaign_id" "$pump_duration_seconds" "$pump_interval_seconds" >"$pump_log_file"
wait "$sse_pid" || true

connected_count="$(grep -c '^event:connected$' "$sse_out_file" || true)"
campaign_event_count="$(grep -c '^event:campaign-event$' "$sse_out_file" || true)"

printf 'Stream check target: campaignId=%s campaignName=%s\n' "$campaign_id" "$campaign_name"
printf 'SSE event counters: connected=%s campaign-event=%s\n' "$connected_count" "$campaign_event_count"
printf 'Pump summary: %s\n' "$(cat "$pump_log_file")"

if [[ "$connected_count" -eq 0 ]]; then
  warn 'SSE stream did not emit connected event.'
  printf '--- SSE Output ---\n%s\n' "$(cat "$sse_out_file")"
  exit 1
fi

if [[ "$campaign_event_count" -eq 0 ]]; then
  warn 'SSE stream did not emit campaign-event entries.'
  printf '--- SSE Output ---\n%s\n' "$(cat "$sse_out_file")"
  exit 1
fi

printf 'SSE stream check passed.\n'
