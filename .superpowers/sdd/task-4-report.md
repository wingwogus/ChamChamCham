# Task 4 Report: Policy Synchronization Service And Async Admin Runner

Date: 2026-07-07
Workspace: `/Users/wingwogus/Projects/ChamChamCham`
Branch: `feat/policy-recommendation-nongupez`

## Result

Implemented Task 4 policy synchronization service, async runner, result models, async config, and focused service tests.

## Changed Files

- `backend/application/src/main/kotlin/com/chamchamcham/application/config/AsyncConfig.kt`
- `backend/application/src/main/kotlin/com/chamchamcham/application/policy/PolicySyncResult.kt`
- `backend/application/src/main/kotlin/com/chamchamcham/application/policy/PolicySyncService.kt`
- `backend/application/src/main/kotlin/com/chamchamcham/application/policy/PolicySyncAsyncRunner.kt`
- `backend/application/src/test/kotlin/com/chamchamcham/application/policy/PolicySyncServiceTest.kt`

## Behavior Implemented

- Admin sync job creation records a `PolicySyncJob` with detected NongupEZ year.
- Detection failure creates a `FAILED` job using current `Year` from the injected `Clock`; it does not delete existing policy data.
- Scheduled sync creates a job and runs it synchronously for application/batch use.
- Existing job execution fetches list rows, upserts `PolicyProgram`, fetches detail, generates card text/tags, and marks successful details recommendable.
- Per-program detail fetch failures are counted as `detailFailureCount`, keep the overall job `SUCCEEDED`, and mark the affected program non-recommendable.
- Whole-source failures during list fetch mark the job `FAILED` without deleting previous data.
- `getJob` maps persisted job state to `PolicySyncResult.JobDetail` and uses the existing `RESOURCE_NOT_FOUND` business exception pattern.
- `PolicySyncAsyncRunner.run(jobId)` delegates to `PolicySyncService.runExistingJob(jobId)` under `@Async`.

## Simplifications Made

- Reused existing domain entity mutation methods instead of adding new persistence abstractions.
- Kept source URL generation private to the service and exact to the NongupEZ detail URL spec.
- Used existing generic `RESOURCE_NOT_FOUND` because Task 4 does not introduce a policy-sync-specific `ErrorCode`.

## Verification

- TDD red attempt:
  - Command: `cd backend && ./gradlew :application:test --tests "com.chamchamcham.application.policy.PolicySyncServiceTest"`
  - Result: failed at `:application:compileTestKotlin`.
  - Evidence: first run included missing `PolicySyncService` references from the new Task 4 test, plus unrelated pre-existing RAG/dev test compilation errors.

- Main compile:
  - Command: `cd backend && ./gradlew :application:compileKotlin`
  - Result: `BUILD SUCCESSFUL`.

- Focused Task 4 test command after implementation:
  - Command: `cd backend && ./gradlew :application:test --tests "com.chamchamcham.application.policy.PolicySyncServiceTest"`
  - Result: blocked before test execution by unrelated existing `DevRagSeedServiceTest` compilation errors.
  - Evidence: unresolved references such as `FarmingRecordDocumentFactory`, `RagProperties`, `DevRagSeedService`, `DevRagSeedCommand`, `VectorStore`, `Document`, and `PdfTextExtractor`.

- Isolated behavior probe:
  - Command: `cd backend && ./gradlew -I /tmp/cham-policy-probe/policy-probe.init.gradle :application:runPolicySyncProbe`
  - Result: `BUILD SUCCESSFUL`, `PolicySyncProbe PASS`.
  - Covered: successful list/detail sync, detail failure success-with-count behavior, detection failure failed-job behavior, exact `detailUrl`, 19-character summary guard, and no `deleteAll` on whole-source failure.

## Known Concerns

- The committed `PolicySyncServiceTest` could not be executed through Gradle because the application test source set is currently blocked by unrelated RAG/dev compilation failures.
- Full `:application:test` remains blocked for the same unrelated reason noted in the task brief.
- No live NongupEZ calls were made.

## Token Usage

Exact token usage was unavailable in this execution surface.
