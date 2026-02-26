#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Emails API seed start'

seed_user_info="$(create_seed_user 'emails-user')"
IFS='|' read -r user_id external_id user_email <<<"$seed_user_info"

template_info="$(create_seed_template 'emails-template')"
IFS='|' read -r template_id template_name <<<"$template_info"

send_payload="$(
  "$JQ_BIN" -cn \
    --argjson templateId "$template_id" \
    --argjson userId "$user_id" \
    '{
      campaignId:null,
      templateId:$templateId,
      templateVersion:null,
      userIds:[$userId]
    }'
)"

send_result='SKIPPED'
if send_response="$(post_json '/emails/send/notifications' "$send_payload" 'emails-send')"; then
  send_result="$(json_value "$send_response" '.data.isSuccess')"
else
  warn 'Immediate email send failed. Template/schedule data creation continues.'
fi

expired_time="$(future_time_iso_local 2)"
schedule_payload="$(
  "$JQ_BIN" -cn \
    --argjson templateId "$template_id" \
    --argjson userId "$user_id" \
    --arg expiredTime "$expired_time" \
    '{
      templateId:$templateId,
      templateVersion:null,
      userIds:[$userId],
      expiredTime:$expiredTime
    }'
)"

schedule_response="$(post_json '/emails/schedules/notifications/email' "$schedule_payload" 'emails-schedule')"
schedule_id="$(json_value "$schedule_response" '.data.newSchedule')"

schedules_response="$(get_json '/emails/schedules/notifications/email')"
histories_response="$(get_json '/emails/histories?page=0&size=20')"

schedule_count="$(json_value "$schedules_response" '.data.schedules | length')"
history_count="$(json_value "$histories_response" '.data.histories | length')"

printf 'Seed user: id=%s externalId=%s email=%s\n' "$user_id" "$external_id" "$user_email"
printf 'Template: id=%s name=%s\n' "$template_id" "$template_name"
printf 'Immediate send result (isSuccess or SKIPPED): %s\n' "$send_result"
printf 'Created schedule id: %s\n' "$schedule_id"
printf 'Email schedules count: %s\n' "$schedule_count"
printf 'Email histories count (page=0,size=20): %s\n' "$history_count"
