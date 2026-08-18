# Development Plan — 5-Day Implementation

Scope: add **audit logging (AOP)**, **JWT refresh token + logout (Redis)**, and an
**event-driven progress-report export (RabbitMQ + Resilience4j)** to the Spring LMS API,
plus full documentation for a separate consumer microservice.

## Current Status

> **Days 1–3 are complete and verified end-to-end (with demo seed data).** As of the end of
> Day 2 the following is in place:
> - Spring Boot **4.1.0** / Spring Framework 7.0.8; `spring-boot-starter-aspectj`
    >   (note: `spring-boot-starter-aop` was **renamed** to `spring-boot-starter-aspectj` in Boot 4),
    >   plus `spring-data-redis`, `resilience4j`, `springdoc 3.0.2` deps (`d3f94ab`)
> - Audit foundation: `AuditLog` entity + repo, `AuditService` (REQUIRES_NEW persistence,
    >   scalar-only reflection snapshot, filterable search), `@Auditable` annotation + `AuditAspect`,
    >   `AuditLogResponseDTO` + `AuditLogController` (`GET /api/audit-logs` filters + pageable,
    >   `GET /api/audit-logs/{id}`), ADMIN only — `5547e68`, `4d953ea`
> - Audit aspect resilience: `safeRecord(...)` helper so a failed audit write never masks the
    >   original exception (both success + failure paths) — `4d953ea`
> - Swagger: audit-logs `Pageable` rendered as optional `page`/`size`/`sort` via `@ParameterObject`
>   + `@PageableDefault(sort = "createdAt", direction = DESC)` — newest-first default;
      >   `@ParameterObject` applied to the other 4 bare-`Pageable` controllers
> - **DB indexes** changeset (`add_indexes.sql`): `users.email` → `VARCHAR(255)` + UNIQUE,
    >   `class_enrollments(class_id, user_id, role)`, `assignment_submissions(assignment_id, user_id)`,
    >   `audit_logs(created_at)` — `87f25fe`
> - **Exception handling** overhaul: `ConflictException` (409), missing Spring/Jackson handlers
    >   (type mismatch, constraint violation, unreadable body, missing param → 400; method → 405;
    >   data integrity → 409), per-field bean-validation errors, generic 500 message (no internals
    >   leaked), 4xx logged as `warn` / 5xx as `error`, `UnAuthenticationHandler` 401-code fix —
    >   `9f17012`
> - **Service exception cleanup**: `RuntimeException` → proper exceptions (MaterialService,
    >   UserService, ProgressService, AuthService); login returns 401 for both unknown email and
    >   wrong password (anti-enumeration) — `583af66`
> - **Day 2 auth hardening** (uncommitted WIP, `JwtUtil`/`AuthService`/`RefreshTokenService`/
    >   `AuthController`/`JwTAuthFilter`/`SecurityConfig` + new `RefreshTokenResponseDTO`):
    >   distinct `accessSecret`/`refreshSecret` (refresh tokens signed + validated with `refreshSecret`),
    >   expiration config as Spring-native `Duration` strings (`access` **30m**, `refresh` **1d**),
    >   non-rotating refresh (new access token only; refresh token valid until 1d expiry or logout),
    >   logout revokes all user refresh tokens in Redis, access-token-only filter guard
> - **Day 3 (RabbitMQ export publisher)** code-complete and verified end-to-end:
    >   `spring-boot-starter-amqp`, `RabbitMQConfig` (exchange/queue/DLQ + explicit `RabbitAdmin`),
    >   `ProgressReportPublisher` with `@CircuitBreaker("rabbitmq-publish")`,
    >   `POST /api/class/{id}/export` → 202 + audited, fallback → 503 (verified, see Day 3 below)
> - **Day 4 (consumer microservice)** complete: the separate `spring-lms-consumer` app was
    >   fully implemented and containerized (Dockerfile + single `docker-compose.yaml` with
    >   consumer + `rabbitmq-lms` + `mailpit-lms`), Mailpit configured with SMTP auth
    >   `admin`/`admin`, and the full flow verified end-to-end (API → RabbitMQ → consumer →
    >   summary/detail CSV → Mailpit; acks vs reject→DLQ). See Day 4 below.
> - **Consumer hardening pass** complete: after the original build, `spring-lms-consumer`
    >   received a code-review hardening pass — soft-delete filtering, CSV formula-injection
    >   escaping, recipient-email validation, circuit-breaker/DLQ rework, N+1 query batching,
    >   observability (actuator + DLQ depth gauge), Docker non-root, and the deferred items
    >   (`Enum`→`enums` rename, removed `User.password`, required SMTP/DB credentials, in-memory
    >   idempotency). All tasks green with 31 tests. See `docs/consumer-service.md` §11 and
    >   `docs/ongoing-plan.md`.
> - **Still pending**: docker-compose polish, admin bootstrap + final docs (Day 5)
>
> **Post–Day 4 hardening (code review + grilling):** Critical→Low findings addressed
> (T1–T23), refresh token rotation added (T22), OSIV disabled, CORS per-profile,
> Swagger gated to dev, rate limiting on auth endpoints, bean validation enforced.
> Day 5 tasks: README, example.env, docker-compose update, architecture doc refresh.
> Grilling session (2026-08-18) decisions: Docker Compose kept lean (user sets up own
> infra), manual seed via Swagger/SQL, git history left as-is, @PreAuthorize bypass skipped.

---

## Day 1 — Finish audit foundation ✅ DONE

| Task | Details | Status |
| ---- | ------- | ------ |
| Audit viewing endpoint | `AuditLogResponseDTO` + `AuditLogController` (`GET /api/audit-logs` with filters `entityType`, `action`, `status`, `userId`, `from`, `to` + pageable; `GET /api/audit-logs/{id}`), ADMIN only | ✅ `5547e68` |
| Aspect resilience polish | Wrap the aspect catch-block `record()` in its own try/catch so a failed audit write never masks the original exception (also applied to success path) | ✅ `4d953ea` |
| Manual verification | `mvn clean package -DskipTests`; start app against local MySQL; via Swagger: create/update/delete a class, material, assignment; register a user; check `audit_logs` rows (success + forced-failure) | ✅ build green, app boots, user registration + audit row verified in DB; full Swagger walkthrough was partially done |

**Deliverable:** full CRUD audit trail visible over the API.

**Extras rolled into Day 1 (from code review):**
- DB indexes (`add_indexes.sql`) — `87f25fe`
- Exception-handler overhaul + `ConflictException` — `9f17012`
- Service exception cleanup + login anti-enumeration — `583af66`

## Day 2 — Auth hardening (refresh + logout + config) ✅ DONE

Auth hardening was finished, compiled, spotless-formatted, and manually verified. Note: the
refresh flow ended up **non-rotating** (new access token only) — a deliberate deviation from
the original "rotation" wording below, confirmed with the user.

| Task | Details | Status   |
| ---- | ------- |----------|
| Security config | `SecurityConfig`: permits only `POST /api/auth` and `POST /api/auth/refresh`; every other route (incl. `logout`) requires auth | ✅       |
| Filter guard | `JwTAuthFilter`: only accepts tokens passing `jwtUtil.validateAccessToken(token)` (access tokens only) | ✅       |
| Separate secrets | `JwtUtil`: distinct `accessSecret`/`refreshSecret`; refresh tokens are signed AND validated with `refreshSecret` | ✅       |
| Expiration config | `jwt.access.expiration: ${JWT_ACCESS_EXPIRATION:30m}` / `jwt.refresh.expiration: ${JWT_REFRESH_EXPIRATION:1d}` — Spring-native `Duration` strings (fixed the old ms/seconds mismatch; refresh typo `JWT_ACCESS_EXPIRATION` → `JWT_REFRESH_EXPIRATION` resolved) | ✅       |
| Refresh response | `POST /api/auth/refresh` → new `RefreshTokenResponseDTO` (`email`, `accessToken`, `expiresIn`); refresh token is NOT rotated/revoked — stays valid until its 1d expiry or logout; new refresh token only via re-login | ✅       |
| Swagger cleanup | `@ParameterObject` applied to all 5 bare-`Pageable` controllers; audit-logs keeps `@PageableDefault(sort = createdAt, DESC)` | ✅       |
| Manual verification | login → `{ accessToken, refreshToken, expiresIn: 1800 }`; refresh → new access token, same refresh token stays valid; logout → refresh fails with "revoked"; Redis stores `refresh:token:*` / `refresh:user:*`; login/refresh/logout audited | ✅       |

**Deliverable:** stateful logout + non-rotating refresh backed by Redis (refresh token stays
valid until 1d expiry or logout; new refresh token issued only by re-login).

> **Update:** Refresh token rotation + reuse detection was added later (T22, `bba9f76`).
> `RefreshTokenService.rotate()` now deletes the consumed jti and stores the new one.
> `RefreshTokenResponseDTO` gained a `refreshToken` field so the rotated token is returned.

## Day 3 — RabbitMQ export publisher (API side) ✅ DONE (verified)

Code is complete, builds green, and the app boots. Verified end-to-end with demo seed data:
the export endpoint returns **202** and the message lands in `progress.report.export.q`;
stopping RabbitMQ opens the circuit and the API returns **503** (see Manual verification).

| Task         | Details                                                                                 |
|--------------|-----------------------------------------------------------------------------------------|
| Dependency   | `spring-boot-starter-amqp` in `pom.xml`                                                 |
| Topology     | `RabbitMQConfig`: direct exchange `progress.exchange`, queue `progress.report.export.q` 
(DLX args → `progress.exchange` / key `progress.report.export.dlq`), DLQ
`progress.report.export.dlq`, bindings, `JacksonJsonMessageConverter` (Jackson 3 —
`Jackson2JsonMessageConverter` is deprecated in Spring AMQP 4.0) + `RabbitTemplate`; explicit
`RabbitAdmin` bean (Boot 4 no longer auto-registers one) |
| Message + publisher | `ProgressReportMessage` (`classId`, `recipientEmail`, `requestedAt`, `requesterName`);
`ProgressReportPublisher.publish(...)` wrapped in `@CircuitBreaker(name = "rabbitmq-publish",
  fallbackMethod = "publishFallback")`; fallback throws `ServiceUnavailableException` → 503
(deviation: the plan wording originally said `BusinessException`) |
| Endpoint | `POST /api/class/{classId}/export` (ADMIN or teacher) → 202 "queued" + audit `action=export` on the class |
| Resilience4j | `resilience4j.circuitbreaker` default config + `rabbitmq-publish` instance in `application.yaml` |
| Infra | docker-compose: add `rabbitmq:3-management` (5672/15672) + env wiring |
| Manual verification | ✅ — `POST /api/class/{id}/export` → 202; payload `{classId, recipientEmail, requestedAt, requesterName}` lands in `progress.report.export.q` (UI 15672); `docker stop rabbitmq` → 5+ attempts @ 50% failure rate → 503 "Progress report export is temporarily unavailable" + app logs "Service unavailable"; recovers ~10s after `docker start rabbitmq` |

**Deliverable:** event-driven export request → durable queue message, resilient to broker outages.

**Extras rolled into Day 3 (from code review):**
- Fixed a Day-2 regression (`6cba4dd`): `SecurityConfig` permitted `POST /api/auth` but the login
  route is `/api/auth/login` — login returned 401. Committed separately as
  `fix(auth): permit POST /api/auth/login`.

## Day 4 — Consumer service documentation + Mailpit ✅ DONE (verified)

The consumer was not only specified but **implemented and verified end-to-end**. Build detail
(the task-by-task spec) lives in `docs/consumer-service.md` in the consumer repo.

| Task | Details | Status |
| ---- | ------- | ------ |
| Consumer spec | Write `docs/consumer-service.md` (full standalone build spec — see below) | ✅ spec written, then turned into a working app |
| Consumer implementation | Domain layer, repositories, messaging/topology, report builder, CSV, mail, listener + resilience circuit breaker — all 9 tasks done (`9c473be`…`20d62db`) | ✅ done + built (jar green) |
| Mailpit | docker-compose: add `mailpit` (SMTP 1025 / UI 8025); document `SMTP_*` env for the consumer | ✅ SMTP auth `admin`/`admin`, plaintext; `SMTP_AUTH=true` required |
| Containerization | `Dockerfile` (two-stage Maven→Temurin-21) + single `docker-compose.yaml` (consumer + `rabbitmq-lms` + `mailpit-lms`) | ✅ verified with `docker compose up` (feature/container `20d62db`) |
| End-to-end wiring notes | Document the complete flow: API publish → RabbitMQ → consumer → summary + detail CSV → Mailpit; ack vs reject→DLQ | ✅ verified live; strategy-B type-mapper round-trip + DB queries + email delivery all confirmed |
| Manual verification | Full local stack (API + RabbitMQ + Mailpit) running; publish a message; confirm it's consumable and the payload round-trips as JSON | ✅ message drained, email with both CSVs in Mailpit, failure → DLQ |

**Deliverable:** the consumer is fully implemented, specified, and the local broker stack is demoable.

**Notes / caveats:**
- **SMTP auth:** Mailpit enforces `admin`/`admin` + plaintext (`MP_SMTP_AUTH_ALLOW_INSECURE`); the
  consumer's `spring.mail.smtp.auth` must be `SMTP_AUTH=true` and `SMTP_USERNAME`/`SMTP_PASSWORD`
  are **required env vars with no fallback** (hardening pass, `docs/consumer-service.md` §8c) — or
  the app fails to start / mailing fails → DLQ.
- **Consumer hardening:** the consumer was later hardened end-to-end (soft-delete, CSV injection,
  email validation, breaker/DLQ, N+1 batching, observability, non-root Docker, idempotency).
  See `docs/consumer-service.md` §11 and `docs/ongoing-plan.md`.
- **Idempotency:** since the consumer runs at-least-once, an in-memory TTL guard
  (`ReportIdempotencyGuard`, 5-min, keyed `classId|recipientEmail|requestedAt`) dedups redelivery
  so a crash-after-send doesn't resend the email. Lossy across restarts — acceptable for the
  current edge-case status.
- **Broker ownership:** this compose stack defines its own RabbitMQ + Mailpit; the API repo's
  compose also declares RabbitMQ on the same ports, so both stacks can't be up at once (container
  names were suffixed `-lms` to reduce collision).
- **Producer needs no change:** `ClassesService.exportProgress` already falls back to the current
  user's email; see the caveat added to "Caveats for future development".

## Day 5 — Docs, admin bootstrap, polish ✅ IN PROGRESS

Day 5 was refined via a grilling session (2026-08-18). Decisions:
- **Docker Compose:** kept lean (API + RabbitMQ only); MySQL and Valkey commented out as optional; user sets up own infrastructure.
- **Seed data:** manual via Swagger/SQL; `scripts/seed.sql` and `scripts/seed.ts` available but not auto-run.
- **Git history:** left as-is (190 commits, honest development noise accepted).
- **@PreAuthorize bypass test:** skipped — portfolio demo, not pen-test.
- **LF renormalization:** skipped — no active development planned.

| Task | Details | Status |
| ---- | ------- | ------ |
| README | `README.md` — project description, tech stack, features, dependencies/tech used, prerequisites, how to run (Maven + Docker Compose), API docs (WIP) | 🔄 In progress (not committed — user revising) |
| `example.env` | All env vars with comments: `DB_*`, `JWT_*` (required), `RABBITMQ_*` / `REDIS_*` (optional) | 🔄 In progress |
| Docker Compose | Update `docker-compose.yaml` — comment out MySQL/Valkey, add notes about consumer repo | 🔄 In progress |
| Architecture doc | Update `docs/architecture.md` — deployment diagram, auth section (rotation, rate limiting), Valkey note, CORS/Swagger/OSIV updates | 🔄 In progress |
| Admin bootstrap | Manual SQL (documented): `UPDATE users SET role = 'ADMIN' WHERE email = '<email>';` | ✅ documented in `docs/architecture.md` |
| `.env` / `example.env` | Finalize all vars | 🔄 In progress (see above) |
| Final verification | `mvn clean package -DskipTests`; spotless formatting; confirm all DB changesets applied | ⏸ After docs are done |

**Deliverable:** polished, documented, demo-ready feature set.

---

## Manual verification script (used each day)

1. `docker compose up -d rabbitmq redis mailpit` — MySQL + Valkey already run as Docker containers (`mysql`, `valkey` on 6379)
2. Start API against local MySQL (`mvn spring-boot:run`) — Liquibase applies pending changesets on startup
3. Demo data: `bun scripts/seed.ts` (docker-exec seed of 8 users / 2 classes / 8 enrollments / 6 materials / 4 assignments / 12 submissions / 30 progress rows; all users password `Passw0rd!`; credentials in `docs/demo-credentials.md`) — or manually register a user → promote to ADMIN via SQL
4. Login → capture tokens → refresh (new access token only; refresh token stays valid) → logout (refresh token revoked)
5. Create class → material → assignment → enroll students → submit → grade
6. Query `/api/audit-logs` (ADMIN) and check the filter queries
7. `POST /api/class/{classId}/export` → observe message in RabbitMQ UI

## Caveats for future development

- **Line endings (CRLF ↔ LF) — biggest gotcha.** Committed files are **CRLF** (edited on Windows/IntelliJ); edits from WSL produce **LF**. Git then treats an entire touched file as one collapsed diff hunk, which silently bundles unrelated changes into a commit (this is how Day-2 WIP ended up inside `583af66`). Before more feature work, do a one-time normalization (see [LF line-ending enforcement](#lf-line-ending-enforcement) below). Review `git diff -w` (ignore whitespace) to see *real* content changes.
- **Uncommitted WIP is the norm, not the exception.** The working tree is intentionally dirty (Day-2 WIP, formatting, docs). Always stage only the files for the current task; double-check `git status` before committing. `docs/`, `RefreshTokenService`, and `RefreshTokenRequestDTO` are untracked — the first docs commit should include `docs/`.
- **DB access.** MySQL and Valkey run in Docker (`docker ps`: `mysql`, `valkey`, `portainer`). There is **no local `mysql` CLI in WSL** — query via `docker exec mysql mysql -u root -p<pass> lmsdb ...`. Writes to MySQL happen only through the demo seed (`bun scripts/seed.ts`) or Liquibase changesets; the assistant should not hand-edit data ad hoc.
- **UUIDs are stored as `BINARY(16)`.** DBeaver renders them as garbled bytes — use `SELECT BIN_TO_UUID(id) ...` to read them. Hibernate generates UUIDv4 client-side; the schema default `UUID_TO_BIN(UUID())` only affects raw SQL inserts, not app writes.
- **Config footguns.** `jwt.*` expirations use Spring-native `Duration` strings (`access` `30m`, `refresh` `1d`) — always include a unit (a bare number means **ms**). The app runs on **8080**; `.env`'s `APP_PORT=8081` is not auto-loaded by Spring (no dotenv dependency) — it only matters when wiring Docker Compose.
- **Swagger + `Pageable`.** A bare `Pageable` controller param renders as a required collapsed object in Swagger. Use `@ParameterObject` (flattens to optional `page`/`size`/`sort`) + `@PageableDefault` for per-endpoint defaults. Audit-logs is fixed; 4 other controllers still use bare `Pageable`.
- **Boot 4 renames.** `spring-boot-starter-aop` → `spring-boot-starter-aspectj`. Watch for other Spring Framework 7 / Boot 4 API renames when adding dependencies.
- **Changelog registration is easy to miss.** `create_audit_logs_table.sql` was added but never registered in `db.changelog-master.yaml` (fixed in `87f25fe`). Always append new scripts to the master changelog, or a fresh DB silently lacks the table.
- **Login security.** Unknown email and wrong password both return 401 — deliberate anti-enumeration. Don't "fix" it back to a 404.
- **JWT placeholders.** The `@Value` keys are `jwt.access.expiration` / `jwt.refresh.expiration` (dotted), matching `application.yaml`. The filter guard accepts **access** tokens only (`validateAccessToken`); refresh tokens (signed with `refreshSecret`) must not be used as bearer credentials.
- **Export recipientEmail — no producer change required.** `ClassesService.exportProgress` (since `7d298ff`) already falls back to the **current user's email** when the request body's `recipientEmail` is null/blank. Verified end-to-end against the consumer: omit `recipientEmail` (or pass the Swagger `"string"` placeholder) and the consumer still delivers to `currentUser.getEmail()`. The consumer itself now **validates** the recipient as a single `InternetAddress` before sending (hardening pass, `MailService.parseSingleRecipient`) and rejects to the DLQ on blank/multi/invalid instead of hitting SMTP 553. Optional hardening on the producer (separate repo, not done): add `@NotBlank @Email` to `ExportRequestDTO.recipientEmail` so a placeholder is rejected with 400 upstream.

## LF line-ending enforcement (deferred)

**Context:** committed files are CRLF (Windows/IntelliJ); edits from WSL produce LF. Git then
treats a touched file as one collapsed diff hunk, silently bundling unrelated changes into
commits (this is how Day-2 WIP landed inside `583af66`). As of the Day-2 checkpoint, the
working tree has **75 modified files: 60 differ from HEAD by line endings only** (verified
`git diff -w` is empty), plus 15 with real content changes.

**Do NOT do this yet** — the 15 Day-2 WIP files are dirty; a repo-wide renormalize now would
bundle WIP into a style commit.

**When to do it:** after Day-2 WIP is committed and the tree is clean.

**Steps:**
1. Update `.gitattributes` → add `* text=auto eol=lf` (keep `*.cmd text eol=crlf`; `mvnw` is
   covered by the global rule). Use `text=auto` (not `text`) so binary files are never corrupted.
2. `git add --renormalize .`; verify `git diff -w --cached` is **empty** (proves zero content
   changes staged); commit `style: enforce LF line endings via .gitattributes`.
3. Once committed, git auto-normalizes staged files to LF — future commits carry no eol noise.
4. Set IntelliJ line separator to LF (Settings → Editor → Code Style → Line separator:
   `Unix and macOS (LF)`) so `git status` stays clean.

## Out of scope

- Tests (per project preference — keep the existing build green with `-DskipTests`)
- Consumer implementation (built in a separate repository from `docs/consumer-service.md`)

> **Update:** 5 integration tests were added post–Day 4 (JWT issuer, JWT active-user, audit
> before/after, auth rate limiting, refresh rotation). Tests use Testcontainers (MySQL + Valkey
> via `GenericContainer`). See `docs/ongoing.md` test policy for full table.
