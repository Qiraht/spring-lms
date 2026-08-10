# Development Plan — 5-Day Implementation

Scope: add **audit logging (AOP)**, **JWT refresh token + logout (Redis)**, and an
**event-driven progress-report export (RabbitMQ + Resilience4j)** to the Spring LMS API,
plus full documentation for a separate consumer microservice.

## Current Status

> **Days 1–2 are complete.** As of the end of Day 2 the following is in place:
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
> - **Still pending**: RabbitMQ + consumer (Days 3–4), docker-compose infra + env wiring
>   (moved to Day 5), docs and admin bootstrap (Day 5)

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

| Task | Details | Status |
| ---- | ------- | ------ |
| Security config | `SecurityConfig`: permits only `POST /api/auth` and `POST /api/auth/refresh`; every other route (incl. `logout`) requires auth | ✅ |
| Filter guard | `JwTAuthFilter`: only accepts tokens passing `jwtUtil.validateAccessToken(token)` (access tokens only) | ✅ |
| Separate secrets | `JwtUtil`: distinct `accessSecret`/`refreshSecret`; refresh tokens are signed AND validated with `refreshSecret` | ✅ |
| Expiration config | `jwt.access.expiration: ${JWT_ACCESS_EXPIRATION:30m}` / `jwt.refresh.expiration: ${JWT_REFRESH_EXPIRATION:1d}` — Spring-native `Duration` strings (fixed the old ms/seconds mismatch; refresh typo `JWT_ACCESS_EXPIRATION` → `JWT_REFRESH_EXPIRATION` resolved) | ✅ |
| Refresh response | `POST /api/auth/refresh` → new `RefreshTokenResponseDTO` (`email`, `accessToken`, `expiresIn`); refresh token is NOT rotated/revoked — stays valid until its 1d expiry or logout; new refresh token only via re-login | ✅ |
| Swagger cleanup | `@ParameterObject` applied to all 5 bare-`Pageable` controllers; audit-logs keeps `@PageableDefault(sort = createdAt, DESC)` | ✅ |
| Manual verification | login → `{ accessToken, refreshToken, expiresIn: 1800 }`; refresh → new access token, same refresh token stays valid; logout → refresh fails with "revoked"; Redis stores `refresh:token:*` / `refresh:user:*`; login/refresh/logout audited | ✅ |

**Deliverable:** stateful logout + non-rotating refresh backed by Redis (refresh token stays
valid until 1d expiry or logout; new refresh token issued only by re-login).

## Day 3 — RabbitMQ export publisher (API side)

| Task | Details |
| ---- | ------- |
| Dependency | `spring-boot-starter-amqp` in `pom.xml` |
| Topology | `RabbitMQConfig`: direct exchange `progress.exchange`, queue `progress.report.export.q`
  (DLX args → `progress.exchange` / key `progress.report.export.dlq`), DLQ
  `progress.report.export.dlq`, bindings, `Jackson2JsonMessageConverter` + `RabbitTemplate` |
| Message + publisher | `ProgressReportMessage` (`classId`, `recipientEmail`, `requestedAt`, `requesterName`);
  `ProgressReportPublisher.publish(...)` wrapped in `@CircuitBreaker(name = "rabbitmq-publish",
  fallbackMethod = "publishFallback")` (fallback throws `BusinessException`) |
| Endpoint | `POST /api/class/{classId}/export` (ADMIN or teacher) → 202 "queued" + audit `action=export` on the class |
| Resilience4j | `resilience4j.circuitbreaker` default config + `rabbitmq-publish` instance in `application.yaml` |
| Infra | docker-compose: add `rabbitmq:3-management` (5672/15672) + env wiring |
| Manual verification | RabbitMQ UI (15672): message lands in `progress.report.export.q`; stop RabbitMQ → API returns 503/fallback after circuit opens |

**Deliverable:** event-driven export request → durable queue message, resilient to broker outages.

## Day 4 — Consumer service documentation + Mailpit

| Task | Details |
| ---- | ------- |
| Consumer spec | Write `docs/consumer-service.md` (full standalone build spec — see below) |
| Mailpit | docker-compose: add `mailpit` (SMTP 1025 / UI 8025); document `SMTP_*` env for the consumer |
| End-to-end wiring notes | Document the complete flow: API publish → RabbitMQ → consumer → summary + detail CSV → Mailpit; ack vs reject→DLQ |
| Manual verification | Full local stack (API + RabbitMQ + Redis + Mailpit) running; publish a message; confirm it's consumable and the payload round-trips as JSON |

**Deliverable:** the consumer is fully specified and the local broker stack is demoable.

## Day 5 — Docs, admin bootstrap, polish

| Task | Details |
| ---- | ------- |
| Admin bootstrap | Manual SQL (documented): `UPDATE users SET role = 'ADMIN' WHERE email = '<email>';` |
| Infra — Redis | docker-compose: add `redis` service (6379) — **note: `valkey` (Redis-compatible) already running in Docker on 6379**; pass `REDIS_HOST`/`REDIS_PORT` to the API service |
| Architecture doc | Update `docs/architecture.md` (ER `audit_logs`, Redis/RabbitMQ/Mailpit/consumer diagrams, admin note) |
| `.env` / `example.env` | Finalize all vars: `REDIS_*`, `JWT_*` (access/refresh secrets + `JWT_ACCESS_EXPIRATION`/`JWT_REFRESH_EXPIRATION`), `RABBITMQ_*`, `SMTP_*` |
| Final verification | `mvn clean package -DskipTests`; end-to-end demo run; spotless formatting (`./mvnw spotless:apply`); confirm all DB changesets applied |

**Deliverable:** polished, documented, demo-ready feature set.

---

## Manual verification script (used each day)

1. `docker compose up -d rabbitmq redis mailpit` — MySQL + Valkey already run as Docker containers (`mysql`, `valkey` on 6379)
2. Start API against local MySQL (`mvn spring-boot:run`) — Liquibase applies pending changesets on startup
3. Register a user → promote to ADMIN via SQL
4. Login → capture tokens → refresh (new access token only; refresh token stays valid) → logout (refresh token revoked)
5. Create class → material → assignment → enroll students → submit → grade
6. Query `/api/audit-logs` (ADMIN) and check the filter queries
7. `POST /api/class/{classId}/export` → observe message in RabbitMQ UI

## Caveats for future development

- **Line endings (CRLF ↔ LF) — biggest gotcha.** Committed files are **CRLF** (edited on Windows/IntelliJ); edits from WSL produce **LF**. Git then treats an entire touched file as one collapsed diff hunk, which silently bundles unrelated changes into a commit (this is how Day-2 WIP ended up inside `583af66`). Before more feature work, do a one-time normalization (see [LF line-ending enforcement](#lf-line-ending-enforcement) below). Review `git diff -w` (ignore whitespace) to see *real* content changes.
- **Uncommitted WIP is the norm, not the exception.** The working tree is intentionally dirty (Day-2 WIP, formatting, docs). Always stage only the files for the current task; double-check `git status` before committing. `docs/`, `RefreshTokenService`, and `RefreshTokenRequestDTO` are untracked — the first docs commit should include `docs/`.
- **DB access (read-only for the assistant).** MySQL and Valkey run in Docker (`docker ps`: `mysql`, `valkey`, `portainer`). There is **no local `mysql` CLI in WSL** — query via `docker exec mysql mysql -u root -p<pass> lmsdb ...`. The assistant may only **read** the DB; all schema changes are applied through **Liquibase changesets** when the app starts.
- **UUIDs are stored as `BINARY(16)`.** DBeaver renders them as garbled bytes — use `SELECT BIN_TO_UUID(id) ...` to read them. Hibernate generates UUIDv4 client-side; the schema default `UUID_TO_BIN(UUID())` only affects raw SQL inserts, not app writes.
- **Config footguns.** `jwt.*` expirations use Spring-native `Duration` strings (`access` `30m`, `refresh` `1d`) — always include a unit (a bare number means **ms**). The app runs on **8080**; `.env`'s `APP_PORT=8081` is not auto-loaded by Spring (no dotenv dependency) — it only matters when wiring Docker Compose.
- **Swagger + `Pageable`.** A bare `Pageable` controller param renders as a required collapsed object in Swagger. Use `@ParameterObject` (flattens to optional `page`/`size`/`sort`) + `@PageableDefault` for per-endpoint defaults. Audit-logs is fixed; 4 other controllers still use bare `Pageable`.
- **Boot 4 renames.** `spring-boot-starter-aop` → `spring-boot-starter-aspectj`. Watch for other Spring Framework 7 / Boot 4 API renames when adding dependencies.
- **Changelog registration is easy to miss.** `create_audit_logs_table.sql` was added but never registered in `db.changelog-master.yaml` (fixed in `87f25fe`). Always append new scripts to the master changelog, or a fresh DB silently lacks the table.
- **Login security.** Unknown email and wrong password both return 401 — deliberate anti-enumeration. Don't "fix" it back to a 404.
- **JWT placeholders.** The `@Value` keys are `jwt.access.expiration` / `jwt.refresh.expiration` (dotted), matching `application.yaml`. The filter guard accepts **access** tokens only (`validateAccessToken`); refresh tokens (signed with `refreshSecret`) must not be used as bearer credentials.

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
