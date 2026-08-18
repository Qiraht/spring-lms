# Ongoing — Spring LMS Improvement Plan

Source: `docs/reports.md` (2026-08-10 review)
Decisions: all findings in report order, small independent commits, security/config kept separate from tests, conflicts sidestepped.

## Current state

Finding **#5 (SpEL boundary leak)** is **already implemented** in the working tree:
- `EnrollmentService.isEnrolledInMaterial` / `isEnrolledInAssignment` added (`EnrollmentService.java:50-63`)
- `AssignmentController` / `MaterialController` / `SubmissionController` updated to use them
- `GlobalExceptionHandler` gained an `@ExceptionHandler(AccessDeniedException.class)`

Everything else below is still open.

## Task status (2026-08-17)

| Task | Title | Status | Feat commit | Test commit |
| ---- | ----- | ------ | ----------- | ----------- |
| T1 | JWT secrets fail fast, no defaults | ✅ Done | `d3b9577` | — |
| T2 | DB_PASSWORD prod typo | ✅ Done | `972fcc7` | — |
| T3 | Rate-limit auth endpoints | ✅ Done | `a538ca9` | `d5b6e70` |
| T4 | Batch N+1 student summaries | ✅ Done | `7432ea8` | `d5b6e70` |
| T6 | Move progress side-effect off GET | ✅ Done | `11d7561` | — |
| T7 | Disable OSIV + eager-load | ✅ Done | `d7520cc` | — |
| T8 | Capture audit before/after diff | ✅ Done | `31328b5` | `d5b6e70` |
| T9 | Per-profile CORS origins | ✅ Done | `8f9697a` | — |
| T10 | Gate Swagger / API docs to dev | ✅ Done | `a6234fb` | — |
| T11 | Jackson 3 ObjectMapper | ✅ Done | `039d298` | — |
| T12 | Stop returning JPA entity | ✅ Done | `6e90514` | — |
| T13 | Validate bulk enroll | ✅ Done | `3c38cda` | — |
| T14 | Validate grade/submission payloads | ✅ Done | `22df518` | — |
| T15 | Fix `server.port` key | ✅ Done | `fb6ae60` | — |
| T16 | JWT active-user check per request | ✅ Done | `56f5f04` | `5fae7d3` |
| T17 | UNIQUE `users.email` | 🗑 Removed | — | — |
| T18 | Created status in response body | ⬜ Open | — | — |
| T19 | Correct filter exclusion list | ⬜ Open | — | — |
| T20 | Failed-login audit (unknown email) | ⬜ Open | — | — |
| T22 | Refresh-token rotation / reuse | ⬜ Open | — | (pending) `test: cover refresh reuse/revocation` |
| T23 | Verify JWT issuer | ⬜ Open | — | (pending) `test: cover JWT issuer validation` |
| T-D1 | Validate `recipientEmail` | ⬜ Open | — | (pending) `test: cover export recipientEmail validation` |
| T-D2 | Add redis service to compose | ⬜ Open | — | — |
| T-D3 | Finalize `example.env` | ⬜ Open | — | — |
| T-D4 | `server.port` (dup of T15) | 🔁 Dup | — | — |
| T-D5 | Flatten bare `Pageable` params | ⬜ Open | — | — |
| T-D6 | Fix `architecture.md` staleness | ⬜ Open | — | — |
| T-D7 | LF line-ending renormalization | ⬜ Open | — | — |
| @PreAuthorize bypass | Assert auth rules per endpoint | ⬜ Open | — | (pending) `test: assert authorization rules per endpoint` |

---



## Critical

### T1 — JWT secrets: fail fast, no hard-coded defaults
- `src/main/resources/application.yaml:28,31`: `${JWT_ACCESS_SECRET:v6yB...}` → `${JWT_ACCESS_SECRET}`, `${JWT_REFRESH_SECRET:89ac...}` → `${JWT_REFRESH_SECRET}`
- Commit: `fix(security): require JWT secrets via env, fail fast`
- **Status: ✅ DONE — `d3b9577`**

### T2 — DB password: fix prod placeholder only (keep dev default)
- `application-prod.yaml:12`: `DB_PASSWORd` → `DB_PASSWORD`
- **Keep** `application-dev.yaml:12` `password: ${DB_PASSWORD:supersecretpassword}` — dev default retained (dev-only, never reused in prod)
- Verify `docker-compose.yaml` passes the same `DB_PASSWORD` (it does)
- Commit: `fix(config): correct DB_PASSWORD typo in prod placeholder`
- **Status: ✅ DONE — `972fcc7`**

---

## High

### T3 — Rate-limit auth endpoints
- `AuthService.LoginUser` / `RefreshToken`: `@RateLimiter(name = "default", fallbackMethod = ...)` + 429 fallback
- Confirmed (2026-08-17): `resilience4j.ratelimiter.configs.default` is declared in `application.yaml` but **no `@RateLimiter` annotation exists**; only `@CircuitBreaker` on `ProgressReportPublisher` is used. The `resilience4j-ratelimiter` jar is on the classpath transitively. T3 is unbuilt. _(Now built — see Status below.)_
- Confirm resilience4j AOP enablement (may need `@EnableAspectJAutoProxy`)
- Commit: `feat(security): rate-limit auth endpoints`
- Test commit: `test: cover auth rate limiting`
- **Status: ✅ DONE — `a538ca9`** (feature) · **`d5b6e70`** (test)

### T4 — Batch N+1 in student progress summaries
- `ProgressService.getAllStudentSummariesForClass` (`:125`)
- Add `GROUP BY user` projections to `StudentProgressRepository` / `AssignmentSubmissionRepository`
- Assemble `StudentClassSummaryDTO` page in memory, single pass
- Commit: `perf: batch student summaries to fix N+1`
- Test commit: `test: add progress summary aggregation regression`
- **Status: ✅ DONE — `7432ea8`** (feature) · **`d5b6e70`** (test)

### T6 — GET must not write (progress side-effect)
- `MaterialController`: new `@PostMapping("/{materialId}/complete")`; make `GET /{materialId}` read-only
- Commit: `fix(api): move progress side-effect off GET`
- **Status: ✅ DONE — `11d7561`**

### T7 — Disable OSIV + eager-load associations
- `application.yaml`: `spring.jpa.open-in-view: false`
- Add `JOIN FETCH` / `@EntityGraph` on user/class lookups used in mapping code
- Commit: `fix(jpa): disable OSIV and eager-load associations`
- **Status: ✅ DONE — `d7520cc`**

### T8 — Fix stale audit `afterState`
- `AuditAspect` / `AuditService`: snapshot before/after in caller's `EntityManager`, same transaction
- Drop `REQUIRES_NEW` from `record`; add separate out-of-tx method if callers need it
- Commit: `fix(audit): capture before/after in caller transaction`
- Test commit: `test: assert audit before/after diff`
- **Status: ✅ DONE — `31328b5`** (feature) · **`d5b6e70`** (test)

---

## Medium

### T9 — Per-profile CORS origins
- `SecurityConfig`: read `app.cors.allowed-origins` (`${CORS_ALLOWED_ORIGINS:http://localhost:5173}`)
- Commit: `fix(security): configure CORS origins per profile`

### T10 — Gate Swagger / API docs to dev
- `SecurityConfig.java:43`: permit `/swagger-ui/**`, `/v3/api-docs/**` only when dev; disable springdoc in prod
- Commit: `fix(security): expose API docs only in dev`

### T11 — Jackson 2 → Jackson 3 (Boot 4)
- `UnAuthenticationHandler`, `CustomAccessDeniedHandler`: inject Spring-managed `tools.jackson` `ObjectMapper`
- `ApiResponse`: migrate annotations to `tools.jackson.annotation`
- Commit: `fix: use Spring-managed Jackson 3 ObjectMapper`
- **Note (implementation):** `tools.jackson.annotation` is *not* on the classpath (only `com.fasterxml.jackson.annotation` 2.x ships transitively via jjwt/springdoc). Boot 4's Jackson 3 `ObjectMapper` honors Jackson 2 annotations for compatibility, so `ApiResponse` keeps `com.fasterxml.jackson.annotation`. The real win (dropping the manual `new ObjectMapper()` in the handlers) is in place.

### T12 — Stop returning JPA entity
- `ClassesController.putClass`: `ApiResponse<Classes>` → `ApiResponse<Void>`
- Commit: `fix(api): stop exposing JPA entity in response`

### T13 — Validate bulk enroll
- `EnrollmentController.enrollUsers`: add `@Valid` on the `List`
- Sidestep: leave `@NotBlank` on `UUID`/`enum` as-is (report wording)
- Commit: `fix(api): enable bean validation on bulk enroll`

### T14 — Validate grade + submission payloads
- `GradeRequestDTO`: `@NotNull @DecimalMin("0")` on `score`
- `SubmissionRequestDTO`: `@NotNull @Size` on `attachment`
- Commit: `fix(api): validate grade/submission payloads`

### T15 — Fix `spring.port` key
- dev + prod yaml: `spring.port` → `server.port: ${PORT:8080}`
- Commit: `fix(config): correct server.port key`

### T16 — JWT filter: log stacktrace + validate active user
- `JwTAuthFilter:40`: log full stack trace
- Validate user still active per request (use injected `UserDetailsServiceImpl` or cached `deletedAt` check)
- Perf consideration: extra DB hit per request; gate behind cache
- Commit: `fix(security): validate active user per request`
- Test commit: `test: cover JWT active-user check`

> **T17 removed** — already done: `add_indexes.sql` adds `VARCHAR(255)` + UNIQUE on `users.email`
> (registered at `db.changelog-master.yaml:22-23`). Partially covered by `UserService` check.

---

## Low

### T18 — Created status in response body
- `AssignmentController:32`: `HttpStatus.OK.value()` → `HttpStatus.CREATED.value()`
- Commit: `fix(api): correct created status in response body`

### T19 — Corregir filter exclusion list
- `JwTAuthFilter.shouldNotFilter`: include `/api/auth/login`
- Commit: `fix(security): correct filter exclusion list`

### T20 — Failed-login audit for unknown email
- `AuthService:59-64`: record failed login even when user row missing
- Sidestep: schema stays `NOT NULL`; only record when a user id resolves
- Commit: `fix(audit): record failed logins`

### T22 — Refresh-token rotation / reuse detection
- `AuthService.RefreshToken` / `RefreshTokenService`: consume/rotate jti in Redis, reject reuse as revocation
- Commit: `feat(security): rotate refresh tokens with reuse detection`
- Test commit: `test: cover refresh reuse/revocation`

### T23 — Verify JWT issuer
- `JwtUtil`: add `requireIssuer("spring-lms-api")` on parse
- Commit: `fix(security): verify JWT issuer`
- Test commit: `test: cover JWT issuer validation`

---

## Test policy

Tests are **not** a separate phase. Each feature task that needs coverage ships **two commits**: the
feature commit, then a separate `test:` commit, so feature vs. test changes stay trackable.

**Test infrastructure — Testcontainers (decided, supersedes earlier H2/no-dep options).**
DB-backed integration tests run against real containers via `spring-boot-testcontainers` +
`@Testcontainers` and `@ServiceConnection` (auto-wires container URLs into the app config).
New **test-scope** deps to add to `pom.xml`:
- `org.springframework.boot:spring-boot-testcontainers`
- `org.testcontainers:junit-jupiter`
- `org.testcontainers:mysql`
- `org.testcontainers:valkey` (already planned for Redis-cache work later; added now)

Covers T4 (aggregation query count) and T8 (audit before/after diff). First test run downloads the
MySQL/valkey images (network required). JUnit/Mockito/AssertJ already present via the Boot 4
granular `-test` starters — no new dependency for those.

**Structure — AAA pattern (mandatory).** Every test method uses Arrange–Act–Assert, one
`// given` / `// when` / `// then` label per phase, blank line between phases, no logic straying
across boundaries.

| Task | Test commit |
| ---- | ----------- |
| T3 | `test: cover auth rate limiting` |
| T4 | `test: add progress summary aggregation regression` |
| T8 | `test: assert audit before/after diff` |
| T16 | `test: cover JWT active-user check` |
| T22 | `test: cover refresh reuse/revocation` |
| T23 | `test: cover JWT issuer validation` |
| T-D1 | `test: cover export recipientEmail validation` |
| @PreAuthorize bypass | `test: assert authorization rules per endpoint` |

Tasks with no behavioral change (config/infra/docs/hygiene) need no test commit:
T1, T2, T6, T7, T9–T15, T18–T20, T-D2…T-D7.

---

## Notes / risks

- T8: audit change lands first, test commit separately.
- T16 adds per-request DB lookup — keep cached.
- T3 needs resilience4j AOP bean enablement — confirm at implementation.
- T4/T8 tests run on Testcontainers (MySQL). First run downloads images.
- T7 (OSIV off) may surface pre-existing lazy-loading bugs — treat any suite failures as findings to resolve, not to revert.
- After each commit: `./mvnw spotless:apply` + `compile` + `test`. Verify build before proceeding.

---

# Doc Reconciliation (2026-08-17)

Source: `docs/development-plan.md`, `docs/architecture.md`, `docs/consumer-service.md`,
cross-checked against the current API repo state.

**Already done / nothing to do:**
- `users.email` UNIQUE + DB indexes — `add_indexes.sql`, registered in `db.changelog-master.yaml` (T17 removed).
- ERP audit table, consumer diagrams, admin bootstrap SQL in `architecture.md` — present.
- Day 1–4 features (audit AOP, refresh/logout Redis, RabbitMQ export publisher) — verified in repo.

**Newly added tasks (doc-derived, pending):**

### T-D1 — Validate `ExportRequestDTO.recipientEmail`
- `dto/request/ExportRequestDTO.java`: add `@NotBlank @Email` on `recipientEmail` (`jakarta.validation.constraints`)
- `ClassesController.exportClassProgress`: add `@Valid` to `@RequestBody(required = false) ExportRequestDTO request`
- Why: doc flags as recommended producer hardening ("not done") — reject bad address 400 instead of SMTP 553 → consumer DLQ
- Commit: `fix(api): validate export recipientEmail`
- Test commit: `test: cover export recipientEmail validation`

### T-D2 — Add `redis` service to `docker-compose.yaml`
- Add `valkey` (or reuse `valkey`) service on 6379; pass `REDIS_HOST`/`REDIS_PORT` into `spring-lms-api`
- Note: `development-plan.md` Day 5 says `valkey` already runs externally on 6379 — decide compose vs external
- Commit: `chore(docker): add redis service to API compose`

### T-D3 — Finalize `example.env`
- Add `REDIS_HOST`/`REDIS_PORT`, `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
- Current file only has `APP_PORT` + `DB_*` + `RABBITMQ_*` (secrets left blank, matching Option B)
- Commit: `chore(env): add REDIS_*/JWT_* to example.env`

### T-D4 — `spring.port` → `server.port` (dup of T15 — handled there)
- `application-dev.yaml:2`, `application-prod.yaml:2` — already tracked as **T15**; no separate work

### T-D5 — Flatten remaining bare `Pageable` controller params
- Verify first: AssignmentController / MaterialController / SubmissionController already use `@ParameterObject`;
  confirm which controllers remain bare (dev-plan's "4 still bare" claim may be stale)
- Apply `@ParameterObject` (+ `@PageableDefault` where sensible)
- Commit: `fix(api): flatten remaining bare Pageable params`

### T-D6 — Fix `docs/architecture.md` staleness
- Fill empty "Audit Logs Actions" table (currently `||`)
- ER diagram: `users.email` `text` → `varchar(255)` (post-`add_indexes.sql`)
- Deployment diagram: reflect redis external (or in API compose after T-D2)
- Commit: `docs(architecture): fix stale sections`

### T-D7 — LF line-ending renormalization (repo hygiene, do last)
- `.gitattributes`: `* text=auto eol=lf` (keep `*.cmd text eol=crlf`)
- `git add --renormalize .`; verify `git diff -w --cached` empty; commit `style: enforce LF line endings via .gitattributes`
- Only after in-flight WIP is committed (per dev-plan's deferred note)

## Notes / risks (doc-derived)

- T-D1 is the only doc-recommended code change with a functional gap; the rest are infra/docs/hygiene.
- T-D2 conflicts with the "valkey already external" reality — surface at implementation.
- T-D5 needs a verify-first pass to avoid touching already-correct files.
- T-D7 must be last to avoid mixing style and logic changes.
```