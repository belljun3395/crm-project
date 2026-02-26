#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

init_seed_env

CONTINUE_ON_ERROR="${CONTINUE_ON_ERROR:-0}"

scripts=(
  seed-users.sh
  seed-segments.sh
  seed-events.sh
  seed-emails.sh
  seed-webhooks.sh
  seed-actions.sh
  seed-journeys.sh
  seed-campaign-dashboard.sh
  seed-audit-logs.sh
)

failures=0

for script_name in "${scripts[@]}"; do
  script_path="${SCRIPT_DIR}/${script_name}"
  log "Running ${script_name}"

  if "$script_path"; then
    log "Completed ${script_name}"
  else
    failures=$((failures + 1))
    warn "Failed ${script_name}"
    if [[ "$CONTINUE_ON_ERROR" != '1' ]]; then
      warn 'Stopping because CONTINUE_ON_ERROR is not enabled.'
      exit 1
    fi
  fi

done

if (( failures > 0 )); then
  warn "Completed with ${failures} failure(s)."
  exit 1
fi

log 'All API seed scripts completed successfully.'
