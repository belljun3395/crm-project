#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

REQUIRED_JAVA_MAJOR=17

detect_java_major() {
  local java_cmd="$1"
  local raw_version=""

  raw_version="$(${java_cmd} -version 2>&1 | awk -F'"' '/version/ {print $2; exit}')"
  if [[ -z "$raw_version" ]]; then
    return 1
  fi

  if [[ "$raw_version" == 1.* ]]; then
    echo "$raw_version" | cut -d'.' -f2
    return 0
  fi

  echo "$raw_version" | cut -d'.' -f1
}

if [[ -n "${OPENAPI_JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$OPENAPI_JAVA_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "[INFO] Using OPENAPI_JAVA_HOME=$JAVA_HOME"
fi

CURRENT_JAVA_MAJOR="$(detect_java_major java || true)"

if [[ -n "$CURRENT_JAVA_MAJOR" && "$CURRENT_JAVA_MAJOR" -ge 26 ]]; then
  if [[ "$(uname -s)" == "Darwin" ]] && [[ -x "/usr/libexec/java_home" ]]; then
    FALLBACK_JAVA_HOME="$(/usr/libexec/java_home -v "${REQUIRED_JAVA_MAJOR}" 2>/dev/null || true)"

    if [[ -z "$FALLBACK_JAVA_HOME" ]]; then
      echo "[FAIL] Detected Java ${CURRENT_JAVA_MAJOR}, but no JDK ${REQUIRED_JAVA_MAJOR} found via /usr/libexec/java_home." >&2
      echo "[FAIL] Install JDK ${REQUIRED_JAVA_MAJOR} or set OPENAPI_JAVA_HOME before running this script." >&2
      exit 1
    fi

    export JAVA_HOME="$FALLBACK_JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "[INFO] Detected Java ${CURRENT_JAVA_MAJOR}; switched to JDK ${REQUIRED_JAVA_MAJOR} for OpenAPI generation."
  fi
fi

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
