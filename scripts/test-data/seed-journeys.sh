#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

log 'Journeys API seed start'

seed_user_info="$(create_seed_user 'journey-user')"
IFS='|' read -r user_id external_id user_email <<<"$seed_user_info"

journey_suffix="$(seed_suffix)"
trigger_event_name="journey-trigger-${journey_suffix}"
journey_name="journey-api-${journey_suffix}"

journey_payload="$(
  "$JQ_BIN" -cn \
    --arg journeyName "$journey_name" \
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
          subject:"[SEED] Journey Step",
          body:"Journey triggered by {{eventName}} for user {{userId}}",
          variables:{from:"journey-seed"},
          retryCount:0
        }
      ]
    }'
)"

journey_response="$(post_json '/journeys' "$journey_payload" 'journeys-create')"
journey_id="$(json_value "$journey_response" '.data.id')"

create_seed_event "$trigger_event_name" "$external_id" >/dev/null
sleep 1

journey_list_response="$(get_json '/journeys')"
execution_response="$(get_json "/journeys/executions?journeyId=${journey_id}")"

journey_count="$(json_value "$journey_list_response" '.data | length')"
execution_count="$(json_value "$execution_response" '.data | length')"

history_count='0'
if [[ "$execution_count" != '0' ]]; then
  execution_id="$(json_value "$execution_response" '.data[0].id')"
  history_response="$(get_json "/journeys/executions/${execution_id}/histories")"
  history_count="$(json_value "$history_response" '.data | length')"
fi

printf 'Seed user: id=%s externalId=%s email=%s\n' "$user_id" "$external_id" "$user_email"
printf 'Journey: id=%s name=%s\n' "$journey_id" "$journey_name"
printf 'Journey list size: %s\n' "$journey_count"
printf 'Journey execution count (journeyId filter): %s\n' "$execution_count"
printf 'Journey execution history count: %s\n' "$history_count"
