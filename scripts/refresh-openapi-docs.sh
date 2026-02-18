#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

cd backend
./gradlew generateOpenApiDocs
mkdir -p ../docs
cp build/openapi.json ../docs/openapi.json

echo "OpenAPI generated at ../docs/openapi.json"

if [[ "${VERIFY_ONLY:-0}" != "1" ]]; then
  exit 0
fi

cd "$REPO_ROOT"

if ! git ls-files --error-unmatch docs/openapi.json >/dev/null 2>&1; then
  echo "[WARN] docs/openapi.json is not tracked yet. Baseline check is skipped for now."
  exit 0
fi

git diff --exit-code -- docs/openapi.json
