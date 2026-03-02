#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Journeys API seed start'

JOURNEY_SYNC_WAIT_SECONDS="${JOURNEY_SYNC_WAIT_SECONDS:-1}"

sleep_for_engine() {
  sleep "$JOURNEY_SYNC_WAIT_SECONDS"
}

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

  post_json '/users' "$payload" 'users-update' >/dev/null
}

create_journey_from_payload() {
  local payload="$1"
  local scope="$2"
  local response
  response="$(post_json '/journeys' "$payload" "$scope")"
  local journey_id
  local journey_name
  journey_id="$(json_value "$response" '.data.id')"
  journey_name="$(json_value "$response" '.data.name')"
  printf '%s|%s\n' "$journey_id" "$journey_name"
}

collect_execution_and_history_count() {
  local journey_id="$1"
  local execution_response
  execution_response="$(get_json "/journeys/executions?journeyId=${journey_id}")"
  local execution_count
  execution_count="$(json_value "$execution_response" '.data | length')"

  local history_count='0'
  if [[ "$execution_count" != '0' ]]; then
    local execution_id
    execution_id="$(json_value "$execution_response" '.data[0].id')"
    local history_response
    history_response="$(get_json "/journeys/executions/${execution_id}/histories")"
    history_count="$(json_value "$history_response" '.data | length')"
  fi

  printf '%s|%s\n' "$execution_count" "$history_count"
}

journey_suffix="$(seed_suffix)"
trigger_event_name="journey-trigger-${journey_suffix}"
segment_token="journey-segment-target-${journey_suffix}"

event_user_info="$(create_seed_user 'journey-event-user')"
IFS='|' read -r event_user_id event_external_id event_user_email <<<"$event_user_info"

segment_user_a_info="$(create_seed_user 'journey-segment-user-a')"
IFS='|' read -r segment_user_a_id segment_user_a_external_id segment_user_a_email <<<"$segment_user_a_info"

segment_user_b_info="$(create_seed_user 'journey-segment-user-b')"
IFS='|' read -r segment_user_b_id segment_user_b_external_id segment_user_b_email <<<"$segment_user_b_info"

segment_info="$(create_seed_segment_for_email 'journey-segment' "$segment_token")"
IFS='|' read -r segment_id segment_name <<<"$segment_info"

event_journey_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "journey-event-${journey_suffix}" \
    --arg triggerEventName "$trigger_event_name" \
    '{
      name:$journeyName,
      triggerType:"EVENT",
      triggerEventName:$triggerEventName,
      active:true,
      steps:[
        {
          stepOrder:1,
          stepType:"ACTION",
          channel:"EMAIL",
          destination:"journey-seed@example.com",
          subject:"[SEED] EVENT Journey",
          body:"event={{eventName}} user={{userId}}",
          variables:{from:"journey-seed-event"},
          retryCount:0
        }
      ]
    }'
)"
IFS='|' read -r event_journey_id event_journey_name <<<"$(create_journey_from_payload "$event_journey_payload" 'journeys-event-create')"

segment_enter_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "journey-segment-enter-${journey_suffix}" \
    --argjson segmentId "$segment_id" \
    '{
      name:$journeyName,
      triggerType:"SEGMENT",
      triggerSegmentId:$segmentId,
      triggerSegmentEvent:"ENTER",
      active:true,
      steps:[
        {
          stepOrder:1,
          stepType:"ACTION",
          channel:"EMAIL",
          destination:"journey-seed@example.com",
          subject:"[SEED] SEGMENT ENTER",
          body:"segment enter user={{userId}}",
          variables:{from:"journey-seed-segment-enter"},
          retryCount:0
        }
      ]
    }'
)"
IFS='|' read -r segment_enter_journey_id segment_enter_journey_name <<<"$(create_journey_from_payload "$segment_enter_payload" 'journeys-segment-enter-create')"

segment_exit_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "journey-segment-exit-${journey_suffix}" \
    --argjson segmentId "$segment_id" \
    '{
      name:$journeyName,
      triggerType:"SEGMENT",
      triggerSegmentId:$segmentId,
      triggerSegmentEvent:"EXIT",
      active:true,
      steps:[
        {
          stepOrder:1,
          stepType:"ACTION",
          channel:"EMAIL",
          destination:"journey-seed@example.com",
          subject:"[SEED] SEGMENT EXIT",
          body:"segment exit user={{userId}}",
          variables:{from:"journey-seed-segment-exit"},
          retryCount:0
        }
      ]
    }'
)"
IFS='|' read -r segment_exit_journey_id segment_exit_journey_name <<<"$(create_journey_from_payload "$segment_exit_payload" 'journeys-segment-exit-create')"

segment_update_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "journey-segment-update-${journey_suffix}" \
    --argjson segmentId "$segment_id" \
    '{
      name:$journeyName,
      triggerType:"SEGMENT",
      triggerSegmentId:$segmentId,
      triggerSegmentEvent:"UPDATE",
      triggerSegmentWatchFields:["user.name","user.email","user.tier"],
      active:true,
      steps:[
        {
          stepOrder:1,
          stepType:"ACTION",
          channel:"EMAIL",
          destination:"journey-seed@example.com",
          subject:"[SEED] SEGMENT UPDATE",
          body:"segment update user={{userId}}",
          variables:{from:"journey-seed-segment-update"},
          retryCount:0
        }
      ]
    }'
)"
IFS='|' read -r segment_update_journey_id segment_update_journey_name <<<"$(create_journey_from_payload "$segment_update_payload" 'journeys-segment-update-create')"

segment_count_reached_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "journey-segment-count-reached-${journey_suffix}" \
    --argjson segmentId "$segment_id" \
    '{
      name:$journeyName,
      triggerType:"SEGMENT",
      triggerSegmentId:$segmentId,
      triggerSegmentEvent:"COUNT_REACHED",
      triggerSegmentCountThreshold:2,
      active:true,
      steps:[
        {
          stepOrder:1,
          stepType:"ACTION",
          channel:"EMAIL",
          destination:"journey-seed@example.com",
          subject:"[SEED] SEGMENT COUNT_REACHED",
          body:"segment count reached={{event.segmentCount}}",
          variables:{from:"journey-seed-segment-count-reached"},
          retryCount:0
        }
      ]
    }'
)"
IFS='|' read -r segment_count_reached_journey_id segment_count_reached_journey_name <<<"$(create_journey_from_payload "$segment_count_reached_payload" 'journeys-segment-count-reached-create')"

segment_count_dropped_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "journey-segment-count-dropped-${journey_suffix}" \
    --argjson segmentId "$segment_id" \
    '{
      name:$journeyName,
      triggerType:"SEGMENT",
      triggerSegmentId:$segmentId,
      triggerSegmentEvent:"COUNT_DROPPED",
      triggerSegmentCountThreshold:2,
      active:true,
      steps:[
        {
          stepOrder:1,
          stepType:"ACTION",
          channel:"EMAIL",
          destination:"journey-seed@example.com",
          subject:"[SEED] SEGMENT COUNT_DROPPED",
          body:"segment count dropped={{event.segmentCount}}",
          variables:{from:"journey-seed-segment-count-dropped"},
          retryCount:0
        }
      ]
    }'
)"
IFS='|' read -r segment_count_dropped_journey_id segment_count_dropped_journey_name <<<"$(create_journey_from_payload "$segment_count_dropped_payload" 'journeys-segment-count-dropped-create')"

create_seed_event "$trigger_event_name" "$event_external_id" >/dev/null
sleep_for_engine

update_seed_user_attributes "$segment_user_a_id" "$segment_user_a_external_id" "${segment_token}-a@example.com" "Segment User A v1" "BETA"
sleep_for_engine

update_seed_user_attributes "$segment_user_a_id" "$segment_user_a_external_id" "${segment_token}-a@example.com" "Segment User A v2" "BETA"
sleep_for_engine

update_seed_user_attributes "$segment_user_b_id" "$segment_user_b_external_id" "${segment_token}-b@example.com" "Segment User B v1" "BETA"
sleep_for_engine

update_seed_user_attributes "$segment_user_b_id" "$segment_user_b_external_id" "outside-${journey_suffix}-b@example.com" "Segment User B v2" "BETA"
sleep_for_engine

update_seed_user_attributes "$segment_user_a_id" "$segment_user_a_external_id" "outside-${journey_suffix}-a@example.com" "Segment User A v3" "BETA"
sleep_for_engine

journey_list_response="$(get_json '/journeys')"
journey_count="$(json_value "$journey_list_response" '.data | length')"

IFS='|' read -r event_exec_count event_history_count <<<"$(collect_execution_and_history_count "$event_journey_id")"
IFS='|' read -r enter_exec_count enter_history_count <<<"$(collect_execution_and_history_count "$segment_enter_journey_id")"
IFS='|' read -r exit_exec_count exit_history_count <<<"$(collect_execution_and_history_count "$segment_exit_journey_id")"
IFS='|' read -r update_exec_count update_history_count <<<"$(collect_execution_and_history_count "$segment_update_journey_id")"
IFS='|' read -r count_reached_exec_count count_reached_history_count <<<"$(collect_execution_and_history_count "$segment_count_reached_journey_id")"
IFS='|' read -r count_dropped_exec_count count_dropped_history_count <<<"$(collect_execution_and_history_count "$segment_count_dropped_journey_id")"

printf 'Event user: id=%s externalId=%s email=%s\n' "$event_user_id" "$event_external_id" "$event_user_email"
printf 'Segment user A: id=%s externalId=%s initialEmail=%s\n' "$segment_user_a_id" "$segment_user_a_external_id" "$segment_user_a_email"
printf 'Segment user B: id=%s externalId=%s initialEmail=%s\n' "$segment_user_b_id" "$segment_user_b_external_id" "$segment_user_b_email"
printf 'Segment: id=%s name=%s token=%s\n' "$segment_id" "$segment_name" "$segment_token"
printf 'Journey list size: %s\n' "$journey_count"
printf 'Journey %s(id=%s) executions=%s histories=%s\n' "$event_journey_name" "$event_journey_id" "$event_exec_count" "$event_history_count"
printf 'Journey %s(id=%s) executions=%s histories=%s\n' "$segment_enter_journey_name" "$segment_enter_journey_id" "$enter_exec_count" "$enter_history_count"
printf 'Journey %s(id=%s) executions=%s histories=%s\n' "$segment_exit_journey_name" "$segment_exit_journey_id" "$exit_exec_count" "$exit_history_count"
printf 'Journey %s(id=%s) executions=%s histories=%s\n' "$segment_update_journey_name" "$segment_update_journey_id" "$update_exec_count" "$update_history_count"
printf 'Journey %s(id=%s) executions=%s histories=%s\n' "$segment_count_reached_journey_name" "$segment_count_reached_journey_id" "$count_reached_exec_count" "$count_reached_history_count"
printf 'Journey %s(id=%s) executions=%s histories=%s\n' "$segment_count_dropped_journey_name" "$segment_count_dropped_journey_id" "$count_dropped_exec_count" "$count_dropped_history_count"
