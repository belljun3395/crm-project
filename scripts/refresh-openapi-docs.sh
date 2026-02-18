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
  echo "[FAIL] docs/openapi.json baseline is missing. Run scripts/refresh-openapi-docs.sh and commit the result." >&2
  exit 1
fi

if ! git diff --exit-code -- docs/openapi.json; then
  echo "[FAIL] OpenAPI spec drift detected. Run scripts/refresh-openapi-docs.sh and commit docs/openapi.json." >&2
  exit 1
fi
