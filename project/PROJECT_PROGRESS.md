# Project Development Progress

This file is the canonical handoff and progress record for this repository.

## Session Rule

- At the start of every new session, read this file before inspecting the rest of the project.
- During each session, update this file with meaningful progress, blockers, verification results, and next actions.
- Keep entries concise and dated so future sessions can continue without asking for a separate progress recap.

## Current Snapshot

- Date: 2026-05-29
- Workspace: `D:\codex\data-monitor`
- Backend database profile uses the shared PostgreSQL database at `192.168.3.2:5432/heimdallr_monitor`.
- Flyway migration `V004__observability_test_stack.sql` exists locally and points Sprint 2 data sources to the shared observability stack.
- Flyway migration `V005__sprint3_alerting_foundation.sql` exists locally for the Sprint 3 alerting foundation.
- Flyway migration `V006__sprint3_alert_runtime.sql` exists locally for persisted alert runtime and evaluation samples.
- Frontend Node.js should be managed with `nvm`; `nvm use 22.12.0` has been verified for Vitest.
- Backend `db` profile was restarted on 2026-05-10 and is listening on `0.0.0.0:8080` with PID `32832`.

## 2026-05-29 Project Completion / Todo Refresh

Current completion:

- Agent workflow is documented with root `AGENTS.md`, `project/AGENT_TEAM.md`, README entry points, and this progress handoff file.
- Sprint 1 core asset, permission, audit, and base API scope is implemented and covered by existing backend/frontend tests.
- Sprint 2 real data-source integration is implemented for Prometheus metrics, Elasticsearch logs/traces, Agent Gateway merge state, actuator Prometheus exposure, and observability verification tooling.
- Sprint 3 alerting foundation is implemented across database migrations, domain records, repositories, service layer, REST API, permissions, audit/history, mock email sender, runtime/sample persistence, scheduled evaluation, notification retry worker, and Alert Workbench frontend.
- Sprint 3 hardening now covers alert filters, terminal-state guards, viewer/write permission denial, cross-scope access denial, active-event JDBC upsert consistency, unsupported-method 405 handling, and due notification retry behavior.

Latest verification baseline:

- Backend Maven test suite passed on 2026-05-29 with JDK `D:\software\jdk-25.0.2`: 15 tests, 0 failures, 0 errors.
- Frontend verification passed on 2026-05-29: `npm run lint`, `npm test` with 3 files / 5 tests, and `npm run build`; the Vite large main chunk warning remains pre-existing.
- Frontend `npm test` and `npm run build` initially hit sandbox EPERM errors while writing Vite/TypeScript temp files under `frontend/node_modules`; rerunning outside the sandbox completed successfully.
- Observability script exists and isolates the current shared-stack blocker: Prometheus can be queried, but the running container still scrapes the stale `host.docker.internal:8080` target until the actual mounted config on `192.168.3.2` is updated.

Prioritized todo:

1. Update the actual Prometheus mounted config on `192.168.3.2` so `heimdallr-api-local` scrapes `192.168.3.11:8080`, then reload/restart Prometheus and rerun `.\scripts\verify-observability-stack.ps1`.
2. Add a real PostgreSQL multi-threaded integration test for concurrent alert evaluation before enabling distributed scheduler workers.
3. Replace the mock email sender with SMTP-backed production delivery, templates, and final retry/failure policy.
4. Decide whether alert rule delete should become a supported API; if yes, implement permission, audit, repository, and frontend behavior; if no, keep the clean 405 contract.
5. Extend Sprint 3 recovery and duration semantics beyond the current immediate evaluation skeleton.
6. Continue Alert Workbench polish after runtime semantics settle: background refresh, richer detail route, and any UX refinements found in product review.

## 2026-05-27 Agent Team Workflow

Completed locally:

- Added root `AGENTS.md` as the repository-level Agent operating guide.
- Added `project/AGENT_TEAM.md` with the Architecture -> Exploration -> Implementation -> Testing -> Review role model, handoff template, routing rules, and verification gates.
- Added README entry points for the Agent team workflow.

Verified on 2026-05-27:

- Documentation-only change; no backend or frontend test command was required.

Next actions:

1. Use `AGENTS.md` at the start of future Agent-assisted sessions.
2. Use `project/AGENT_TEAM.md` to split larger feature work into focused Agent responsibilities.

## Sprint 1 Status

- Asset, permission, audit, and core API implementation has test coverage.
- Backend Maven tests previously passed: 9 tests green.
- Frontend Vitest tests passed after switching nvm to Node `22.12.0`: 2 files, 4 tests green.
- Sprint 1 is not the main remaining blocker for real data source integration.

## Sprint 2 Real Data Source Integration Status

Completed or locally implemented:

- `JdbcMonitorData` now validates data sources through real HTTP probes.
- Metric query path attempts Prometheus `/api/v1/query_range` before falling back to fixture samples.
- Log query path attempts Elasticsearch `test-logs-*` before falling back to database fixture logs.
- Elasticsearch trace lookup uses `traceId.keyword` exact matching.
- Agent list merges mock Agent Gateway `/status` and can report gateway-backed online state.
- `heimdallr-api` includes `micrometer-registry-prometheus`, enabling `/actuator/prometheus`.
- `deploy/observability/prometheus/prometheus.yml` is updated to scrape this developer host at `192.168.3.11:8080`.

Verified on 2026-05-10:

- Backend `/health` returned `UP`.
- Backend `/actuator/prometheus` returned HTTP `200`.
- Backend `/api/v1/me` with `Bearer admin-token` returned the platform admin user.
- Data source validation `POST /api/v1/data-sources/ds-prom-prod/validate` returned `PASSED`.
- Metric query `POST /api/v1/metrics/query` with `broker_up` and `obj-kafka-orders` returned 31 Prometheus samples from `ds-prom-prod`.
- Log search `POST /api/v1/logs/search` with `traceId=trace-codex-bridge-001` returned exactly 1 Elasticsearch-backed log row.
- Agent list `GET /api/v1/agents` returned 4 agents, all merged to `ONLINE` from Agent Gateway status.
- Agent Gateway: `http://192.168.3.2:18080/status` returned `gateway=ready`, `online=3`.
- Elasticsearch: `http://192.168.3.2:9200/_cluster/health` returned `yellow`.
- Elasticsearch trace evidence: `traceId.keyword=trace-codex-bridge-001` returned exactly 1 document from `test-logs-*`.
- Prometheus itself is reachable and reports `agent-gateway` and `prometheus` targets as up.
- Correct scrape endpoint for this developer host, `http://192.168.3.11:8080/actuator/prometheus`, returned HTTP `200`.
- Re-verified later on 2026-05-10:
  - Old backend PID `20588` was no longer running; `db` profile was restarted outside the sandbox after sandboxed Java networking to PostgreSQL failed with `Permission denied: getsockopt`.
  - Backend `/health` returned `UP` from both `http://localhost:8080/health` and `http://192.168.3.11:8080/health`.
  - Backend `/actuator/prometheus` returned HTTP `200` from both `localhost:8080` and `192.168.3.11:8080`.
  - `netstat -ano | findstr :8080` showed `0.0.0.0:8080` and `[::]:8080` listening with PID `32832`.
  - Prometheus readiness `http://192.168.3.2:9090/-/ready` returned `Prometheus Server is Ready.`
  - Prometheus query endpoint `http://192.168.3.2:9090/api/v1/query?query=up` returned HTTP `200`.
  - Prometheus active targets still showed `agent-gateway` and `prometheus` as `up`, and `heimdallr-api-local` as `down`.
  - `heimdallr-api-local` scrape URL in active targets was still `http://host.docker.internal:8080/actuator/prometheus`; latest error remained `server returned HTTP status 404 Not Found`.
  - Reloading `http://192.168.3.2:9090/-/reload` succeeded, but `/api/v1/status/config` still reported `host.docker.internal:8080`, proving the running Prometheus is not reading this workspace's updated `deploy/observability/prometheus/prometheus.yml`.
  - Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 9 tests, 0 failures, 0 errors.
  - Follow-up verification confirmed `http://192.168.3.11:8080/actuator/prometheus` returns HTTP `200`, while Prometheus active targets still scrape `http://host.docker.internal:8080/actuator/prometheus`.
  - Management access probes from this workstation did not find a usable direct path to the `192.168.3.2` host: local `docker` CLI is unavailable, and TCP probes for `22`, `445`, `2375`, and `5985` did not complete successfully.
  - Added `scripts/verify-observability-stack.ps1` so the live Prometheus target, backend scrape endpoint, and expected `heimdallr-api-local` health can be rechecked after the actual mounted config is changed.
  - Verification script was run successfully; it passed backend health, backend Prometheus endpoint, Prometheus readiness, and active target retrieval, then failed only on the expected current blocker: `heimdallr-api-local` still scrapes `http://host.docker.internal:8080/actuator/prometheus` and remains `down`.
  - Backend Maven tests were re-run and passed: 9 tests, 0 failures, 0 errors.
  - Frontend verification was re-run after a small Fast Refresh cleanup in `PermissionGate`: `npm run lint` passed with no warnings, `npm test` passed 2 files / 4 tests, and `npm run build` passed. Vite still reports the existing large bundle warning for the main chunk.

Still open:

- Running Prometheus still loads an older target, `host.docker.internal:8080`, for `heimdallr-api-local`.
- Running Prometheus reports `heimdallr-api-local` as down. Latest observed error: `server returned HTTP status 404 Not Found`.
- Reloading `http://192.168.3.2:9090/-/reload` did not pick up this repository's updated `deploy/observability/prometheus/prometheus.yml`; the live Prometheus container is mounted from another directory on `192.168.3.2` or the mounted file was not updated there.

## Sprint 3 Alerting Foundation Status

Started on 2026-05-10.

Completed or locally implemented:

- Added `V005__sprint3_alerting_foundation.sql` with `alert_rule`, `alert_event`, `alert_event_history`, `notification_record`, `on_call_group`, and `on_call_group_member`.
- Added alert permissions `alerts:read` and `alerts:write` to seed roles and local token fixtures.
- Added alert domain records for rules, events, event history, notification records, and on-call groups.
- Added `/api/v1/alerts` backend APIs for rule CRUD, rule enable/disable, manual single-rule and enabled-rule batch evaluation, event list/state transitions, event history, notification records, and on-call groups.
- Added a replaceable `EmailNotificationSender` interface plus mock email sender.
- Manual evaluation uses existing metric query data, applies single metric/object/operator/threshold rules, deduplicates active events, records notification delivery, and writes alert/audit history for key state changes.
- In-memory repository and JDBC repository both implement the Sprint 3 alerting slice.
- Added a frontend Alert Workbench at `/alerts`, including event handling, rule list, rule creation, rule enable/disable, manual evaluation, event history, notification records, on-call group selection, route wiring, menu entry, API services, mock data, and domain types.

Verified on 2026-05-10:

- Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 10 tests, 0 failures, 0 errors.
- New test covers rule creation, enablement, threshold evaluation, notification record creation, acknowledge, processing, close, and event history.
- Frontend verification passed after the Alert Workbench slice: `npm run lint` passed, `npm test` passed 3 files / 5 tests, and `npm run build` passed. Vite still reports the existing large bundle warning for the main chunk.

Still open:

- Detection is intentionally an API-triggered evaluation skeleton; background due-rule scanning, distributed locking, and runtime sample tables are not implemented yet.
- Email delivery is a mock implementation; SMTP configuration, templates, retry worker, and final failure retry policy remain future work.
- Recovery is supported in the data/service path, but the current API test focuses on the manual handling state flow.
- The Alert Workbench is a first usable frontend slice; edit-existing-rule UX, advanced filters, background refresh, and dedicated alert detail route are still open.

Next actions:

1. On the actual machine/directory running Prometheus at `192.168.3.2`, change the `heimdallr-api-local` target from `host.docker.internal:8080` to `192.168.3.11:8080`.
2. Reload or restart Prometheus and confirm `heimdallr-api-local` becomes up in `/api/v1/targets?state=active`.
3. Run `.\scripts\verify-observability-stack.ps1` from the repository root and record the final Prometheus target evidence in this file.
4. Continue Sprint 3 by adding a background alert scheduler, persisted rule runtime/sample records, SMTP-backed sender, retry worker, and richer recovery tests.
5. Extend the Alert Workbench with edit-rule flow, richer filters, and a direct alert detail route once the scheduler/runtime model lands.

## 2026-05-14 Sprint 3 Alert Runtime Slice

Completed locally:

- Added `V006__sprint3_alert_runtime.sql` with `alert_rule_runtime` and `alert_evaluation_sample` for persisted rule runtime and evaluation samples.
- Added alert runtime/sample domain records and repository methods for due-rule discovery, runtime lookup, sample lookup, and evaluation recording.
- Added `AlertEvaluationJob` with local single-instance `@Scheduled` due-rule scanning. It uses a system alert runtime user and intentionally does not introduce distributed locking.
- Manual single-rule evaluation now writes runtime/sample records for matched, recovered, normal, no-data, and failed outcomes while keeping the existing API behavior.
- Added lightweight read APIs: `GET /api/v1/alerts/rules/{id}/runtime`, `GET /api/v1/alerts/rules/{id}/samples`, plus `POST /api/v1/alerts/rules/evaluate-due` for due-rule batch execution.
- Updated the Sprint 3 backend test to verify rule creation, enablement, manual evaluation, persisted runtime, persisted sample, notification record, and event state transitions.

Verified on 2026-05-14:

- Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 10 tests, 0 failures, 0 errors.

Still open:

- Scheduler is a local skeleton with no distributed locking by design.
- Duration/recovery-duration semantics remain simplified; current behavior preserves the existing immediate manual threshold evaluation contract.
- Email delivery remains mock-backed; SMTP templates and retry worker are still future work.

## 2026-05-25 Sprint 3 Hardening

Completed locally:

- Reviewed the Sprint 3 quality findings and verified that notification triggering now uses case-insensitive status checks, so db-profile `triggered` events can still move through notification handling.
- Added backend state-machine guards in both in-memory and JDBC repositories so terminal alert events (`closed` / `recovered`) cannot be acknowledged or moved back to processing.
- Added backend support for alert rule and event filters used by the frontend Alert Workbench: severity, enabled state, and keyword for rules; status, severity, and keyword for events.
- Extended the Sprint 3 backend API test to cover frontend/backend filter alignment and illegal terminal-state transition rejection.
- Confirmed the Alert Workbench frontend includes edit-rule UX, event/rule filters, handling-note/close-reason modal, event history, notification records, and log/metric jump links.

Verified on 2026-05-25:

- Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 10 tests, 0 failures, 0 errors.
- Frontend verification passed after switching from Node `12.22.12` to Node `22.12.0`: `npm run lint` passed, `npm test` passed 3 files / 5 tests, and `npm run build` passed. Vite still reports the existing large bundle warning for the main chunk.

Still open:

- Concurrency-safe active-event upsert under db profile still deserves a repository/integration test before multi-worker scheduling is enabled.
- Viewer/write-denial and cross-scope alert access tests are now covered in the security test entry below.
- SMTP delivery, notification templates, and retry worker remain future work.

## 2026-05-25 Sprint 3 Security / Permission Tests

Completed locally:

- Added an `alert-viewer-token` fixture principal with `alerts:read` but without `alerts:write`.
- Added backend API coverage proving an alert viewer can read visible alert rules but cannot create, update, enable, disable, manually evaluate, due-evaluate, or transition alert events.
- Added cross-scope alert tests proving a trade-scoped user cannot list, read, update, enable, evaluate, inspect runtime/samples/history, receive notification records for, or transition a content-scoped alert rule/event.
- Fixed in-memory alert rule enable/disable to require `alerts:write`.
- Fixed alert rule upsert in both in-memory and JDBC repositories so a user cannot overwrite an existing hidden rule by reusing its id with a visible object.

Verified on 2026-05-25:

- Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 13 tests, 0 failures, 0 errors.

Still open:

- Alert rule delete is not currently an implemented API surface; delete-specific permission coverage should be added when that endpoint exists.
- A DELETE request to an existing rule route currently surfaced as a 500 through the global exception handler during exploratory testing instead of a clean 405. This is outside the alert permission slice but worth hardening later.
- Concurrency-safe active-event upsert under db profile still deserves a repository/integration test before multi-worker scheduling is enabled.
- SMTP delivery, notification templates, and retry worker remain future work.

## 2026-05-25 Sprint 3 Alert Data Consistency

Completed locally:

- Reworked `JdbcMonitorData.upsertTriggeredAlert` to use a single PostgreSQL upsert against the active alert dedup partial unique index instead of the previous select-then-`ON CONFLICT (id)` flow.
- The JDBC upsert now targets `ON CONFLICT (dedup_key) WHERE deleted_at IS NULL AND status NOT IN ('recovered', 'closed')`, updates the active event in place, and returns the canonical event id plus whether the row was newly inserted.
- Alert history and audit rows are now written only when the database reports a newly inserted active event, avoiding duplicate trigger history during repeated evaluations.
- Added an API regression check that repeated manual evaluation reuses the same active event id.
- Added a JDBC SQL regression test to lock the partial-index conflict target and prevent accidental fallback to id-based conflict handling.

Verified on 2026-05-25:

- Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 13 tests, 0 failures, 0 errors.

Still open:

- A real PostgreSQL multi-threaded integration test for concurrent scheduler workers is still desirable before enabling distributed alert evaluation workers.
- Viewer/write-denial and cross-scope alert access tests are now covered in the security test entry above.
- SMTP delivery, notification templates, and retry worker remain future work.

## 2026-05-25 Sprint 3 Notification Retry / API Hardening

Completed locally:

- Added a focused global exception handler branch for unsupported HTTP methods so unsupported routes such as `DELETE /api/v1/alerts/rules/{id}` now return a clean HTTP 405 instead of falling through to a generic 500.
- Added an API regression test for unsupported alert rule delete behavior.
- Added repository contracts for due notification retries and retry-result recording.
- Implemented due notification retry support in both in-memory and JDBC repositories. Retry scans only failed notification records whose events are still in `notification_failed`, honors retry limits, and updates the original notification record instead of creating duplicate retry rows.
- Added `AlertService.retryDueNotifications`, a manual `POST /api/v1/alerts/notifications/retry-due` endpoint, and a scheduled notification retry worker using `heimdallr.alert.notification-retry.*` timing properties.
- Added service-level coverage proving a due failed notification can be resent successfully and moves the alert event back to `NOTIFIED`.
- Extended alert viewer permission coverage so read-only alert users cannot manually trigger notification retries.

Verified on 2026-05-25:

- Backend Maven tests passed with JDK `D:\software\jdk-25.0.2`: 15 tests, 0 failures, 0 errors.

Still open:

- SMTP delivery is still mock-backed; production sender configuration and templates remain future work.
- A real PostgreSQL multi-threaded integration test for concurrent scheduler workers is still desirable before enabling distributed alert evaluation workers.
- Alert rule delete is not currently an implemented API surface; delete-specific permission coverage should be added when that endpoint exists.
