#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Segments API seed start'

segment_token="segment-target-$(seed_suffix)"
seed_user_info="$(create_seed_user "$segment_token")"
IFS='|' read -r user_id external_id user_email <<<"$seed_user_info"

segment_info="$(create_seed_segment_for_email 'segment-api' "$segment_token")"
IFS='|' read -r segment_id segment_name <<<"$segment_info"

segment_list_response="$(get_json '/segments?limit=50')"
segment_detail_response="$(get_json "/segments/${segment_id}")"

segment_count="$(json_value "$segment_list_response" '.data | length')"
segment_condition_count="$(json_value "$segment_detail_response" '.data.conditions | length')"

printf 'Seed user: id=%s externalId=%s email=%s\n' "$user_id" "$external_id" "$user_email"
printf 'Created segment: id=%s name=%s\n' "$segment_id" "$segment_name"
printf 'Segments API list size: %s\n' "$segment_count"
printf 'Created segment condition count: %s\n' "$segment_condition_count"
