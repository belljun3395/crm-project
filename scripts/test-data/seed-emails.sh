#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Emails API seed start'

update_seed_user_attributes() {
  local user_id="$1"
  local external_id="$2"
  local email="$3"
  local name="$4"
  local tier="${5:-BETA}"

  local attributes
  attributes="$(
    "$JQ_BIN" -cn \
      --arg email "$email" \
      --arg name "$name" \
      --arg tier "$tier" \
      '{email:$email,name:$name,tier:$tier}'
  )"

  local payload
  payload="$(
    "$JQ_BIN" -cn \
      --argjson id "$user_id" \
      --arg externalId "$external_id" \
      --arg userAttributes "$attributes" \
      '{id:$id,externalId:$externalId,userAttributes:$userAttributes}'
  )"

  post_json '/users' "$payload" 'emails-users-update' >/dev/null
}

seed_user_info="$(create_seed_user 'emails-user-primary')"
IFS='|' read -r user_id external_id user_email <<<"$seed_user_info"
segment_user_info="$(create_seed_user 'emails-user-segment')"
IFS='|' read -r segment_user_id segment_external_id segment_user_email <<<"$segment_user_info"

segment_token="emails-segment-$(seed_suffix)"
update_seed_user_attributes "$segment_user_id" "$segment_external_id" "${segment_token}@example.com" "Email Segment User"

linked_segment_info="$(create_seed_segment_for_email 'emails-linked-segment' "$segment_token")"
IFS='|' read -r linked_segment_id linked_segment_name <<<"$linked_segment_info"
unlinked_segment_info="$(create_seed_segment_for_email 'emails-unlinked-segment' "emails-unlinked-$(seed_suffix)")"
IFS='|' read -r unlinked_segment_id unlinked_segment_name <<<"$unlinked_segment_info"

campaign_name="emails-campaign-$(seed_suffix)"
campaign_payload="$(
  "$JQ_BIN" -cn \
    --arg name "$campaign_name" \
    --argjson segmentId "$linked_segment_id" \
    '{
      name:$name,
      properties:[],
      segmentIds:[$segmentId]
    }'
)"
campaign_response="$(post_json '/campaigns' "$campaign_payload" 'emails-campaign')"
campaign_id="$(json_value "$campaign_response" '.data.id')"

campaign_event_payload="$(
  "$JQ_BIN" -cn \
    --arg name "emails-campaign-event-$(seed_suffix)" \
    --arg externalId "$segment_external_id" \
    --arg campaignName "$campaign_name" \
    '{
      name:$name,
      externalId:$externalId,
      properties:[],
      campaignName:$campaignName
    }'
)"
post_json '/events' "$campaign_event_payload" 'emails-campaign-event' >/dev/null

template_info="$(create_seed_template 'emails-template')"
IFS='|' read -r template_id template_name <<<"$template_info"

send_user_payload="$(
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
send_user_result='SKIPPED'
if send_user_response="$(post_json '/emails/send/notifications' "$send_user_payload" 'emails-send-user')"; then
  send_user_result="$(json_value "$send_user_response" '.data.isSuccess')"
else
  warn 'Immediate email send(userIds) failed. Remaining scenarios continue.'
fi

send_segment_campaign_payload="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_id" \
    --argjson templateId "$template_id" \
    --argjson segmentId "$linked_segment_id" \
    '{
      campaignId:$campaignId,
      templateId:$templateId,
      templateVersion:null,
      segmentId:$segmentId,
      userIds:[]
    }'
)"
send_segment_campaign_result='SKIPPED'
if send_segment_campaign_response="$(post_json '/emails/send/notifications' "$send_segment_campaign_payload" 'emails-send-segment-campaign')"; then
  send_segment_campaign_result="$(printf '%s\n' "$send_segment_campaign_response" | "$JQ_BIN" -r '.data.isSuccess')"
else
  warn 'Immediate email send(campaign+segment) failed. Remaining scenarios continue.'
fi

# NOTE:
# This payload intentionally uses an unlinked segmentId to validate server-side campaign/segment link checks.
# Failure is expected and should be reported as EXPECTED_FAILURE below.
invalid_send_payload="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_id" \
    --argjson templateId "$template_id" \
    --argjson segmentId "$unlinked_segment_id" \
    '{
      campaignId:$campaignId,
      templateId:$templateId,
      templateVersion:null,
      segmentId:$segmentId,
      userIds:[]
    }'
)"
invalid_send_expected_failure='N/A'
if request_json 'POST' '/emails/send/notifications' "$invalid_send_payload" \
  -H "Idempotency-Key: $(make_idempotency_key 'emails-send-invalid')" >/dev/null; then
  invalid_send_expected_failure='UNEXPECTED_SUCCESS'
else
  invalid_send_expected_failure='EXPECTED_FAILURE'
fi

expired_time_1="$(future_time_iso_local 2)"
schedule_payload_1="$(
  "$JQ_BIN" -cn \
    --argjson templateId "$template_id" \
    --argjson userId "$user_id" \
    --arg expiredTime "$expired_time_1" \
    '{
      templateId:$templateId,
      templateVersion:null,
      userIds:[$userId],
      expiredTime:$expiredTime
    }'
)"
schedule_response_1="$(post_json '/emails/schedules/notifications/email' "$schedule_payload_1" 'emails-schedule-user')"
schedule_id_1="$(json_value "$schedule_response_1" '.data.newSchedule')"

expired_time_2="$(future_time_iso_local 3)"
schedule_payload_2="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_id" \
    --argjson templateId "$template_id" \
    --argjson segmentId "$linked_segment_id" \
    --arg expiredTime "$expired_time_2" \
    '{
      campaignId:$campaignId,
      templateId:$templateId,
      templateVersion:null,
      userIds:[],
      segmentId:$segmentId,
      expiredTime:$expiredTime
    }'
)"
schedule_response_2="$(post_json '/emails/schedules/notifications/email' "$schedule_payload_2" 'emails-schedule-segment-campaign')"
schedule_id_2="$(json_value "$schedule_response_2" '.data.newSchedule')"

# NOTE:
# This payload intentionally uses an unlinked segmentId for schedule API validation checks.
# Failure is expected and should be reported as EXPECTED_FAILURE below.
invalid_schedule_payload="$(
  "$JQ_BIN" -cn \
    --argjson campaignId "$campaign_id" \
    --argjson templateId "$template_id" \
    --argjson segmentId "$unlinked_segment_id" \
    --arg expiredTime "$(future_time_iso_local 4)" \
    '{
      campaignId:$campaignId,
      templateId:$templateId,
      templateVersion:null,
      userIds:[],
      segmentId:$segmentId,
      expiredTime:$expiredTime
    }'
)"
invalid_schedule_expected_failure='N/A'
if request_json 'POST' '/emails/schedules/notifications/email' "$invalid_schedule_payload" \
  -H "Idempotency-Key: $(make_idempotency_key 'emails-schedule-invalid')" >/dev/null; then
  invalid_schedule_expected_failure='UNEXPECTED_SUCCESS'
else
  invalid_schedule_expected_failure='EXPECTED_FAILURE'
fi

schedules_response="$(get_json '/emails/schedules/notifications/email')"
histories_response="$(get_json '/emails/histories?page=0&size=20')"

schedule_count="$(json_value "$schedules_response" '.data.schedules | length')"
history_count="$(json_value "$histories_response" '.data.histories | length')"

printf 'Seed users: primary(id=%s), segment(id=%s)\n' "$user_id" "$segment_user_id"
printf 'Segments: linked(id=%s name=%s) unlinked(id=%s name=%s)\n' \
  "$linked_segment_id" "$linked_segment_name" "$unlinked_segment_id" "$unlinked_segment_name"
printf 'Campaign: id=%s name=%s\n' "$campaign_id" "$campaign_name"
printf 'Template: id=%s name=%s\n' "$template_id" "$template_name"
printf 'Immediate send result(userIds): %s\n' "$send_user_result"
printf 'Immediate send result(campaign+segment): %s\n' "$send_segment_campaign_result"
printf 'Immediate send invalid(link mismatch): %s\n' "$invalid_send_expected_failure"
printf 'Created schedule ids: %s %s\n' "$schedule_id_1" "$schedule_id_2"
printf 'Schedule invalid(link mismatch): %s\n' "$invalid_schedule_expected_failure"
printf 'Email schedules count: %s\n' "$schedule_count"
printf 'Email histories count (page=0,size=20): %s\n' "$history_count"
