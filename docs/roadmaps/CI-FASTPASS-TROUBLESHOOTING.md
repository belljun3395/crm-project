# CI Fast-Pass Troubleshooting (Working Notes)

Purpose: speed up PR green checks during active issue lanes.

Scope: local working guide for current workflow (#210/#211 lessons).

## 1) Pre-push fast checklist

- Use JDK 17 explicitly before backend commands.
- Run `ktlintCheck` first (fast fail).
- Re-sync OpenAPI docs when API/filter/header behavior changes.
- Run targeted tests for changed modules before full CI wait.
- Rebase branch on latest `main` before final push for Ready.

Recommended local sequence:

```bash
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./backend/gradlew ktlintCheck
bash scripts/refresh-openapi-docs.sh

# Example targeted runs (adjust to touched modules)
./backend/gradlew test --tests "com.manage.crm.user.controller.UserControllerIdempotencyIntegrationTest"
./backend/gradlew test --tests "com.manage.crm.event.controller.EventControllerIdempotencyIntegrationTest"
```

## 2) Known failure patterns and immediate fixes

### A. `backend-test` fails with missing `buildSrc.jar`

Symptom:
- `Could not resolve all artifacts for configuration ':buildSrc:buildScriptClasspath'`
- `FileNotFoundException ... backend/buildSrc/build/libs/buildSrc.jar`

Cause:
- Test job ran with `--no-rebuild`, but artifact did not include required `buildSrc.jar`.

Fix:
- Remove `--no-rebuild` from test step in `.github/workflows/validation.yml` for backend test execution.

### B. `backend-openapi-drift` fails

Symptom:
- Drift check fails after controller/filter/config changes.

Common cause:
- `docs/openapi.json` not regenerated and committed with API behavior/header updates.

Fix:
- Run `bash scripts/refresh-openapi-docs.sh`.
- Commit updated `docs/openapi.json` together with API contract changes.

### C. `backend-build-and-lint` fails on Ktlint import order

Symptom:
- `Imports must be ordered in lexicographic order ...`

Fix:
- Reorder imports in failing file (no blank-line grouping hacks).
- Re-run `./backend/gradlew ktlintCheck`.

### D. New idempotency integration tests fail with `409` unexpectedly

Symptom:
- Expected `201/200`, got `409 Conflict` on first call.

Cause:
- Reused static idempotency keys across tests or runs.

Fix:
- Generate unique keys per test invocation (e.g., append `System.currentTimeMillis()`).

## 3) CI signal reading guide

- Treat job result as source of truth; annotation-only SQL messages may appear even when job passes.
- If `backend-openapi-drift` is green, annotation text alone is not a merge blocker.
- For blockers, always inspect failing job logs directly:

```bash
gh run view <run-id> --repo belljun3395/crm-project --log-failed
```

## 4) Lane rule for faster merge

- Keep PR in Draft while iterating on CI fixes.
- Move to Ready only after required checks are green on latest SHA.
- Avoid batching unrelated fixes in one push near Ready; split by concern:
  - CI infra/workflow fix
  - code/test fix
  - openapi/doc sync

## 5) Quick command snippets

Check PR status quickly:

```bash
gh pr view <pr-number> --repo belljun3395/crm-project --json isDraft,mergeStateStatus,statusCheckRollup
gh run list --repo belljun3395/crm-project --branch <branch> --limit 3
gh run view <run-id> --repo belljun3395/crm-project
```

---

Maintainer note:
- This file is intended as live working notes for current lanes.
- Keep concise, tactical, and based on real failures we hit.
