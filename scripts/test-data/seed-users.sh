#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

USER_COUNT="${USER_COUNT:-5}"

log "Users API seed start (count=${USER_COUNT})"

created_ids=()
for ((index = 1; index <= USER_COUNT; index++)); do
  user_info="$(create_seed_user "user-api-${index}")"
  IFS='|' read -r user_id external_id user_email <<<"$user_info"
  created_ids+=("$user_id")
  log "created user id=${user_id} externalId=${external_id} email=${user_email}"
done

users_response="$(get_json '/users?page=0&size=20')"
count_response="$(get_json '/users/count')"

listed_count="$(json_value "$users_response" '.data.users | length')"
total_count="$(json_value "$count_response" '.data.totalCount')"

printf 'Created user IDs: %s\n' "${created_ids[*]}"
printf 'Users API list size (page=0,size=20): %s\n' "$listed_count"
printf 'Users API total count: %s\n' "$total_count"
