#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

USER_COUNT="${USER_COUNT:-8}"
USER_UPDATE_COUNT="${USER_UPDATE_COUNT:-3}"

log "Users API seed start (count=${USER_COUNT}, updates=${USER_UPDATE_COUNT})"

created_ids=()
created_external_ids=()
created_emails=()

for ((index = 1; index <= USER_COUNT; index++)); do
  user_info="$(create_seed_user "user-api-${index}")"
  IFS='|' read -r user_id external_id user_email <<<"$user_info"
  created_ids+=("$user_id")
  created_external_ids+=("$external_id")
  created_emails+=("$user_email")
  log "created user id=${user_id} externalId=${external_id} email=${user_email}"
done

actual_update_count="$USER_UPDATE_COUNT"
if (( actual_update_count > USER_COUNT )); then
  actual_update_count="$USER_COUNT"
fi

for ((index = 0; index < actual_update_count; index++)); do
  user_id="${created_ids[$index]}"
  external_id="${created_external_ids[$index]}"
  current_email="${created_emails[$index]}"

  updated_name="Updated Seed User $((index + 1))"
  updated_tier='VIP'
  updated_plan="PLAN_$((index + 1))"

  updated_attributes="$(
    "$JQ_BIN" -cn \
      --arg email "$current_email" \
      --arg name "$updated_name" \
      --arg tier "$updated_tier" \
      --arg plan "$updated_plan" \
      '{email:$email,name:$name,tier:$tier,plan:$plan}'
  )"

  update_payload="$(
    "$JQ_BIN" -cn \
      --argjson id "$user_id" \
      --arg externalId "$external_id" \
      --arg userAttributes "$updated_attributes" \
      '{id:$id,externalId:$externalId,userAttributes:$userAttributes}'
  )"

  post_json '/users' "$update_payload" 'users-update' >/dev/null
done

users_response="$(get_json '/users?page=0&size=20')"
users_page2_response="$(get_json '/users?page=1&size=20')"
count_response="$(get_json '/users/count')"
query_target="${created_external_ids[0]}"
query_response="$(get_json "/users?page=0&size=20&query=$(url_encode "$query_target")")"

listed_count="$(json_value "$users_response" '.data.users | length')"
listed_page2_count="$(json_value "$users_page2_response" '.data.users | length')"
total_count="$(json_value "$count_response" '.data.totalCount')"
query_count="$(json_value "$query_response" '.data.users | length')"

printf 'Created user IDs: %s\n' "${created_ids[*]}"
printf 'Updated user count: %s\n' "$actual_update_count"
printf 'Users API list size (page=0,size=20): %s\n' "$listed_count"
printf 'Users API list size (page=1,size=20): %s\n' "$listed_page2_count"
printf 'Users API total count: %s\n' "$total_count"
printf 'Users API query count (query=%s): %s\n' "$query_target" "$query_count"
