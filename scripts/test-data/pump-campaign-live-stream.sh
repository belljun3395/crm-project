#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

campaign_id="${1:-}"
duration_seconds="${2:-30}"
interval_seconds="${3:-1}"

if [[ -z "$campaign_id" ]]; then
  echo "Usage: bash scripts/test-data/pump-campaign-live-stream.sh <campaignId> [durationSeconds=30] [intervalSeconds=1]" >&2
  exit 1
fi

if [[ ! "$campaign_id" =~ ^[0-9]+$ ]]; then
  echo "campaignId must be a positive integer: $campaign_id" >&2
  exit 1
fi

if [[ ! "$duration_seconds" =~ ^[0-9]+$ ]] || [[ "$duration_seconds" -le 0 ]]; then
  echo "durationSeconds must be a positive integer: $duration_seconds" >&2
  exit 1
fi

if [[ ! "$interval_seconds" =~ ^[0-9]+$ ]] || [[ "$interval_seconds" -le 0 ]]; then
  echo "intervalSeconds must be a positive integer: $interval_seconds" >&2
  exit 1
fi

campaign_response="$(get_json "/campaigns/${campaign_id}")"
campaign_name="$(json_value "$campaign_response" '.data.name')"

user_a_info="$(create_seed_user 'stream-user-a')"
IFS='|' read -r _ user_a_external_id _ <<<"$user_a_info"
user_b_info="$(create_seed_user 'stream-user-b')"
IFS='|' read -r _ user_b_external_id _ <<<"$user_b_info"

events_for_user_a=(signup open click purchase)
events_for_user_b=(signup open purchase)
idx_a=0
idx_b=0
end_epoch=$(( $(date +%s) + duration_seconds ))
sent_count=0

log "Live stream pump start (campaignId=${campaign_id}, campaignName=${campaign_name}, duration=${duration_seconds}s, interval=${interval_seconds}s)"

while (( $(date +%s) < end_epoch )); do
  create_seed_event "${events_for_user_a[$idx_a]}" "$user_a_external_id" "$campaign_name" >/dev/null
  idx_a=$(( (idx_a + 1) % ${#events_for_user_a[@]} ))
  sent_count=$((sent_count + 1))

  create_seed_event "${events_for_user_b[$idx_b]}" "$user_b_external_id" "$campaign_name" >/dev/null
  idx_b=$(( (idx_b + 1) % ${#events_for_user_b[@]} ))
  sent_count=$((sent_count + 1))

  sleep "$interval_seconds"
done

printf 'Live stream pump completed: campaignId=%s campaignName=%s sentEvents=%s\n' \
  "$campaign_id" "$campaign_name" "$sent_count"
