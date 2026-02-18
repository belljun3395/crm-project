#!/usr/bin/env bash
set -euo pipefail

echo "Checking backend UseCase/Domain contract documentation and test mapping"

BASE_SHA="${GITHUB_BASE_SHA:-}"
if [[ -z "${BASE_SHA}" && -n "${GITHUB_BASE_REF:-}" ]]; then
  BASE_SHA="origin/${GITHUB_BASE_REF}"
fi
HEAD_SHA="${GITHUB_SHA:-HEAD}"

if [[ -n "${BASE_SHA}" ]] && ! git rev-parse --verify "${BASE_SHA}^{commit}" >/dev/null 2>&1; then
  BASE_SHA=""
fi

if [[ -z "${BASE_SHA}" ]]; then
  if git rev-parse --verify "HEAD~1" >/dev/null 2>&1; then
    BASE_SHA="HEAD~1"
  else
    BASE_SHA="HEAD"
  fi
fi

CHANGED_FILES=()
while IFS= read -r file; do
  CHANGED_FILES+=("$file")
done < <(git diff --name-only "${BASE_SHA}...${HEAD_SHA}" -- '*.kt' || true)

if [[ ${#CHANGED_FILES[@]} -eq 0 ]]; then
  echo "No Kotlin file changes found between ${BASE_SHA}...${HEAD_SHA}"
  exit 0
fi

TARGET_FILES=()
for file in "${CHANGED_FILES[@]}"; do
  if [[ "$file" =~ ^backend/src/main/kotlin/com/manage/crm/.*/application/.+UseCase\.kt$ ]] || \
     [[ "$file" =~ ^backend/src/main/kotlin/com/manage/crm/.*/domain/.+\.kt$ ]]; then
    TARGET_FILES+=("$file")
  fi
done

if [[ ${#TARGET_FILES[@]} -eq 0 ]]; then
  echo "No changed UseCase/Domain Kotlin files found."
  exit 0
fi

HAS_ERROR=0

for file in "${TARGET_FILES[@]}"; do
  if ! sed -n '1,80p' "$file" | grep -Eq '^\s*/\*\*'; then
    echo "[FAIL] KDoc missing in first 80 lines: $file"
    HAS_ERROR=1
    continue
  fi

  USECASE_NAME="$(basename "$file" .kt)"
  RELATED_TESTS=()
  while IFS= read -r test_file; do
    [[ -n "$test_file" ]] && RELATED_TESTS+=("$test_file")
  done < <(find backend/src/test/kotlin -type f -name "*${USECASE_NAME}*Test.kt" | sort)

  if [[ ${#RELATED_TESTS[@]} -eq 0 ]]; then
    echo "[WARN] No test file found for changed UseCase/Domain: $USECASE_NAME ($file)"
    continue
  fi

  CONTRACT_TEST_COUNT=0
  for test_file in "${RELATED_TESTS[@]}"; do
    if grep -Eq '(UC|DM)-[A-Z]+-[0-9]+' "$test_file"; then
      CONTRACT_TEST_COUNT=$((CONTRACT_TEST_COUNT + 1))
    fi
  done

  if [[ "$CONTRACT_TEST_COUNT" -eq 0 ]]; then
    echo "[WARN] Contract ID (UC/DM-xxx-###) not found in related tests for $USECASE_NAME"
    echo "[WARN] Related tests: ${RELATED_TESTS[*]}"
  fi
done

if [[ "$HAS_ERROR" -ne 0 ]]; then
  echo "Contract governance check failed. See errors above."
  exit 1
fi

echo "Contract governance check passed."
