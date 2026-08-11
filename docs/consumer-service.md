# Consumer Service Build Plan

This document is a standalone specification for **`spring-lms-consumer`**, a separate
application (own repository) that consumes export messages from RabbitMQ, builds student
progress reports, and emails them. It is written so the service can be built independently
of the API repo.

## Overview

```
API (spring-lms)                         CONSUMER (spring-lms-consumer)
  POST /api/class/{classId}/export          @RabbitListener(progress.report.export.q)
        | publish ProgressReportMessage           |
        v                                          v
   progress.exchange  ──────────────────►  validate message
        | routing key                              | read shared MySQL (read-only)
   progress.report.export.q ─────────────────────►  build aggregate + detail
                                                     | summary.csv + detail.csv
                                                     v
                                                     MailService (JavaMailSender)
                                                       | SMTP → Mailpit (1025) / real SMTP
   on failure ──► reject(requeue=false) ──► progress.report.export.dlq
```

- Publisher: `spring-lms` API (see `docs/development-plan.md`, Day 3).
- Consumer: this spec, built in a separate repo.
- Both share the same MySQL database; the consumer only **reads** progress data and
  **writes nothing** (email is sent externally).

## 1. Project scaffold

- Spring Boot **4.1.0**, Java **21**, Maven, package `com.qiraht.spring_lms_consumer`
- Dependencies:
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-amqp`
  - `spring-boot-starter-mail`
  - `com.mysql:mysql-connector-j` (runtime)
  - `spring-boot-starter-validation`
  - `org.projectlombok:lombok`
  - `spring-cloud-starter-circuitbreaker-resilience4j`
  - `com.opencsv:opencsv` **optional** — otherwise write CSV with a plain `StringBuilder`

## 2. Data-access layer (copy from the API repo)

Copy the entities **exactly** as defined in `src/main/java/com/qiraht/spring_lms/entity`:

| Entity | Table | Notes |
| ------ | ----- | ----- |
| `User` | `users` | used for names + email |
| `Classes` | `classes` | class context |
| `Enrollment` | `class_enrollments` | student roster |
| `Assignment` | `assignments` | detail rows |
| `Material` | `materials` | detail rows |
| `AssignmentSubmission` | `assignment_submissions` | submitted / score |
| `StudentProgress` | `student_progress` | material/assignment completion |
| enum `ClassRole` | — | TEACHER / STUDENT |

Copy the repositories and their queries used by the aggregation logic:

- `EnrollmentRepository.findByClassesIdAndRole(classId, STUDENT, pageable)` — roster;
  add a non-paged variant `findByClassesIdAndRole(classId, role)` for the full export.
- `MaterialRepository.countByClassesId(classId)` and `findByClassesId(classId)`
- `AssignmentRepository.countByClassesId(classId)` and `findByClassesId(classId)`
- `StudentProgressRepository.countCompletedMaterialsByUserIdAndClassId(userId, classId)`
- `StudentProgressRepository.findByUserIdAndMaterialId(userId, materialId)`
- `StudentProgressRepository.findByUserIdAndAssignmentId(userId, assignmentId)`
- `AssignmentSubmissionRepository.countByUserIdAndClassId(userId, classId)`
- `AssignmentSubmissionRepository.getAverageScoreByUserIdAndClassId(userId, classId)`
- `AssignmentSubmissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)`

## 3. Messaging contract (MUST match the API)

| Item | Value |
| ---- | ----- |
| Exchange | `progress.exchange` (type `direct`, durable) |
| Main queue | `progress.report.export.q` (durable) |
| Routing key | `progress.report.export` |
| DLQ | `progress.report.export.dlq` (durable) |
| DLX binding | `progress.exchange` / key `progress.report.export.dlq` |
| Main queue DLX args | `x-dead-letter-exchange` = `progress.exchange`, `x-dead-letter-routing-key` = `progress.report.export.dlq` |
| Message format | JSON (Jackson), `ProgressReportMessage` |

**Message payload** — MUST mirror the API's actual DTO shape (`spring-lms` publishes a
Lombok class with `UUID classId`, not a `record` with `String`):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReportMessage {
    private UUID classId;
    private String recipientEmail;
    private LocalDateTime requestedAt;
    private String requesterName;
}
```

**Type-id resolution (decided: separate repos, strategy B).** The API serializes with
`JacksonJsonMessageConverter`, which stamps a `__TypeId__` header of
`com.qiraht.spring_lms.dto.ProgressReportMessage` on every message. The consumer lives in its
own package (`com.qiraht.spring_lms_consumer.dto`), so it must teach its converter to resolve
that foreign type-id:

- Configure a `JacksonJsonMessageConverter` (Spring AMQP 4.0 name — `Jackson2JsonMessageConverter`
  is deprecated) whose Jackson-3 `DefaultJacksonJavaTypeMapper` uses
  `setIdClassMapping(Map.of("com.qiraht.spring_lms.dto.ProgressReportMessage",
  ProgressReportMessage.class))` (note: the Jackson-3 mapper is `DefaultJacksonJavaTypeMapper`,
  **not** `DefaultJackson2JavaTypeMapper`, which belongs to the deprecated Jackson-2 family).
- Declare the topology with `@Bean` methods (idempotent) in `RabbitMQConfig` (exchange, main
  queue + DLX args, DLQ, bindings, `RabbitAdmin`). A single `MessageConverter` bean is auto-applied
  to Spring Boot's listener container factory (same pattern as the API's publish side), so an
  explicit `SimpleRabbitListenerContainerFactory` is only needed if auto-wiring does not happen.

## 4. Consumer flow

`@RabbitListener(queues = "progress.report.export.q", ackMode = "MANUAL")`

1. **Validate** message fields (non-blank `classId`, `recipientEmail`). Invalid → reject → DLQ.
2. **Build aggregate** (one row per student, reuse the API's `ProgressService` math):
   - class name, student full name
   - total materials, completed materials
   - total assignments, submitted assignments
   - average score (`BigDecimal`, 2 decimals)
   - completion % = `(completedMaterials + submittedAssignments) / (totalMaterials + totalAssignments) * 100`
3. **Build detail** (one row per material/assignment per student):
   - type (`MATERIAL` / `ASSIGNMENT`), title, due date (assignments), status/completed, score
4. **Generate CSV** (Strategy B — two attachments):
   - `summary.csv` — header + aggregate rows
   - `detail.csv` — header + detail rows
5. **Email** via `MailService`: `MimeMessageHelper` with subject
   `"Progress Report — <className>"`, plain-text body, both CSVs as attachments.
6. **Ack** on success: `channel.basicAck(deliveryTag, false)`.
7. **On failure:** log + `channel.basicReject(deliveryTag, false)` → message routed to DLQ.

Wrap the whole `process(message, channel, tag)` body in a Resilience4j circuit breaker:

```java
@CircuitBreaker(name = "report-export", fallbackMethod = "processFallback")
public void process(ProgressReportMessage message, Channel channel, long deliveryTag) throws IOException { ... }

public void processFallback(ProgressReportMessage message, Channel channel, long deliveryTag, Throwable t) {
    log.error("Export failed, sending to DLQ", t);
    channel.basicReject(deliveryTag, false);
}
```

## 5. Mail service

```java
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public void sendReport(String to, String subject, String text,
                           List<ByteArrayResource> attachments, List<String> filenames) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        for (int i = 0; i < attachments.size(); i++) {
            helper.addAttachment(filenames.get(i), attachments.get(i));
        }
        mailSender.send(message);
    }
}
```

## 6. Configuration templates

`application.yaml`
```yaml
spring:
  application:
    name: spring-lms-consumer
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:lmsdb}?allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:supersecretpassword}
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        format-sql: true
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    listener:
      simple:
        acknowledge-mode: manual
        default-requeue-rejected: false
  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:1025}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: ${SMTP_AUTH:false}
          starttls:
            enable: ${SMTP_STARTTLS:false}

resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
    instances:
      report-export:
        base-config: default
```

`example.env`
```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=lmsdb
DB_USER=root
DB_PASSWORD=supersecretpassword
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
SMTP_HOST=localhost
SMTP_PORT=1025
```

docker-compose service snippet — RabbitMQ and Mailpit are defined in this same Compose file;
only MySQL is external (shared with the API):
```yaml
services:
  spring-lms-consumer:
    build: .
    container_name: spring-lms-consumer
    env_file: .env
    environment:
      - DB_HOST=localhost
      - RABBITMQ_HOST=rabbitmq
      - SMTP_HOST=mailpit
    restart: always

  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    restart: always

  mailpit:
    image: axllent/mailpit
    container_name: mailpit
    ports:
      - "1025:1025"
      - "8025:8025"
    restart: always
```

## 7. Verification (manual)

1. Start RabbitMQ + Mailpit (and the API).
2. Publish an export via `POST /api/class/{classId}/export` (teacher/ADMIN token).
3. In RabbitMQ UI (15672): message arrives in `progress.report.export.q`, then is consumed
   (messages drained) and the DLQ stays empty.
4. Open Mailpit UI (8025): email with `summary.csv` + `detail.csv` attachments appears.
5. Force a failure (e.g., bad classId) → message lands in `progress.report.export.dlq`.
6. Requeue from the DLQ after fixing → email sent.

## 8. Future improvements

- Per-student self-service export (`studentId` in payload → email to the student).
- Excel output (Apache POI) with real `Summary` / `Detail` tabs.
- Retry exchange (bounded retries before DLQ) instead of immediate DLQ on failure.

---

## 9. Implementation checklist (in order)

Build decisions locked in before coding:

- **Repos stay separate** (strategy B). Consumer DTO is `com.qiraht.spring_lms_consumer.dto.ProgressReportMessage`
  with `UUID classId`; the API FQCN is resolved via a custom type mapper (§3).
- **Scope: whole repo** — Java app + `application.yaml` + `example.env` + `Dockerfile` +
  `docker-compose.yaml` (consumer + rabbitmq + mailpit).
- **CSV outsourced to plain `StringBuilder`** (`opencsv` declared optional — skipped).
**Build status:** ✅ Tasks 0–8 done; Task 9 (test) pending. Tasks 0–7 committed on `develop`,
Task 8 (containerization) on `feature/container`.

| Task | Commit |
|------|--------|
| 0 dependency review | `9c473be` (with Task 1) |
| 1 domain layer | `9c473be` |
| 2 repositories | `f146520` |
| 3 messaging + topology | `fcd33db` |
| 4 report builder | `3efa1f1` |
| 5 CSV generation | `19d0220` |
| 6 mail service | `5be57eb` |
| 7 listener + resilience | `18c1b8d` |
| 8 configuration + infra | `20d62db` (feature/container) |

- **Dependency review done** ✅. Aligned with the spec §1 list. Changes applied to `pom.xml`:
  - removed `spring-boot-starter-webmvc` (+`-test`) — the consumer is a **listener-only** app,
    no embedded Tomcat needed;
  - added `spring-boot-starter-jackson` explicitly — `spring-rabbit` does NOT bring Jackson
    transitively, but `JacksonJsonMessageConverter` (AMQP 4.0, Jackson 3) needs `tools.jackson`
    databind, which `spring-boot-starter-jackson` provides;
  - collapsed the 5 granular test starters into one `spring-boot-starter-test`;
  - added the Spotless (Palantir) plugin exactly as in the API repo's `pom.xml`
    (`com.diffplug.spotless:2.43.0`, palantir 2.39.0 PALANTIR, `removeUnusedImports`,
    UNIX line endings) — required by `docs/coding-rules.md`.
  Verified: `./mvnw compile`, `./mvnw test-compile`, and `./mvnw spotless:check` all pass.

| # | Task | Files | Verify |
|---|------|-------|--------|
| 1 | ✅ **Domain layer.** Copy entities + enums from `spring-lms`, re-pointing package to `com.qiraht.spring_lms_consumer.{entity,Enum}`: `User`, `Classes`, `Enrollment`, `Assignment`, `Material`, `AssignmentSubmission`, `StudentProgress`, `ClassRole`, `UserRole`. Keep Lombok/JPA annotations exact (`@GeneratedValue(UUID)`, `@CreationTimestamp`, LAZY `@ManyToOne`, `@SQLRestriction` on `Enrollment`). No `AuditLog` (unused). | `src/main/java/com/qiraht/spring_lms_consumer/{entity,Enum}/*.java` | ✅ `./mvnw -q compile` green (`9c473be`) |
| 2 | ✅ **Repositories.** Copy the aggregation repos from `spring-lms` (same derived `@Query`s) + add non-paged variants: `EnrollmentRepository.findByClassesIdAndRole(classId, role)` → `List`, `MaterialRepository.findByClassesId(classId)` → `List`, `AssignmentRepository.findByClassesId(classId)` → `List`. Plus `ClassesRepository` for `classes.name`. **Caveat:** only aggregation-needed methods were kept (lean deviation from a full API copy — paged/`existsBy`/`findByAssignmentId` variants omitted). | `.../repository/*.java` | ✅ `./mvnw -q compile` green (`f146520`). Runtime query resolution still pending (Task 9) |
| 3 | ✅ **Messaging contract + queue topology.** Consumer `ProgressReportMessage` (Lombok, `UUID classId`); `RabbitMQConfig` mirroring the API: `DirectExchange progress.exchange`, queue `progress.report.export.q` (DLX args: exchange + key `progress.report.export.dlq`), DLQ, both bindings, `RabbitAdmin`, `JacksonJsonMessageConverter` with Jackson-3 `DefaultJacksonJavaTypeMapper#idClassMapping` (API FQCN → consumer class). No `RabbitTemplate` (consumer doesn't publish). **Caveats:** `DefaultJacksonJavaTypeMapper` is correct for Jackson 3 (doc fixed); runtime boot + topology-in-UI not yet verified. | `.../dto/ProgressReportMessage.java`, `.../config/RabbitMQConfig.java` | ✅ `./mvnw -q compile` green; `spotless:check` clean (`fcd33db`). Runtime check (boot + topology in UI 15672) deferred to Task 8/9 |
| 4 | ✅ **Report builder.** `ProgressReportService` reusing the API's `ProgressService.getStudentClassSummary` math exactly: roster (`findByClassesIdAndRole(STUDENT)`), per-student totals/completed, avg score (`BigDecimal` 2dp `HALF_UP`, zero when null), completion % `(completedMaterials+submittedAssignments)/(totalMaterials+totalAssignments)*100` (guard div-by-zero). Detail rows: materials via `findByUserIdAndMaterialId` (title, completed); assignments via `findByAssignmentIdAndUserId` (score) + `findByUserIdAndAssignmentId` (completed) + dueDate. Output `SummaryRow` / `DetailRow` DTOs + `ProgressReport` record. **Decisions:** `DetailRow` includes `studentName` (detail spans all students — needed), method is `@Transactional(readOnly = true)` (keeps lazy session open, still writes nothing), missing class → `IllegalArgumentException` (bad-classId → DLQ path). | `.../service/ProgressReportService.java`, `.../dto/{SummaryRow,DetailRow,ProgressReport}.java` | ✅ `./mvnw -q compile` green; `spotless:check` clean (`3efa1f1`). Numbers-vs-API/summary + DLQ flows deferred to Task 9 |
| 5 | ✅ **CSV generation.** `CsvService` with plain `StringBuilder` (quote fields containing `,` `"` `\n`): `summary.csv` (header + aggregate rows), `detail.csv` (header + detail rows). `completionPercentage`/`averageScore` to 2dp, `dueDate` as `yyyy-MM-dd HH:mm`, `completed` as `true`/`false`. | `.../service/CsvService.java`, `.../dto/CsvFile.java` | ✅ `./mvnw -q compile` green; `spotless:check` clean (`19d0220`). Output inspected in Mailpit |
| 6 | ✅ **Mail service.** `MailService` per spec §5: `MimeMessageHelper` (UTF-8, multipart), subject `"Progress Report — <className>"`, plain-text body, both CSVs attached. Signature uses `List<CsvFile>` (deviation from spec's parallel lists, agreed). | `.../service/MailService.java` | ✅ Email lands in Mailpit (8025) with both attachments (`5be57eb`) |
| 7 | ✅ **Listener + resilience.** `ProgressReportConsumer`: `@RabbitListener(queues="progress.report.export.q", ackMode="MANUAL")`; validate (blank `classId`/`recipientEmail` → reject → DLQ); `@CircuitBreaker(name="report-export", fallbackMethod="processFallback")`; success → `basicAck`; failure → log + `basicReject(deliveryTag, false)`. | `.../service/ProgressReportConsumer.java` | ✅ End-to-end verified: message consumed + email delivered (Mailpit) + bad-recipient/mail-down → DLQ (`18c1b8d`) |
| 8 | ✅ **Configuration + infra.** `application.yaml` reconciled to spec §6 (datasource read-only `ddl-auto: none`, rabbit listener manual-ack + `default-requeue-rejected: false`, mail, `resilience4j` `report-export`); `example.env` (+ `SMTP_AUTH=true` note); `Dockerfile` (two-stage Maven→Temurin-21, no EXPOSE); single `docker-compose.yaml` (consumer + `rabbitmq-lms` + `mailpit-lms`). **Compose points:** consumer `DB_HOST=host.docker.internal` + `extra_hosts: host-gateway`; `.env`/`.env.*` gitignored. | `src/main/resources/application.yaml`, `example.env`, `Dockerfile`, `docker-compose.yaml`, `.gitignore` | ✅ `./mvnw clean package -DskipTests` green; `docker compose config` valid; `docker compose up` works (`20d62db`, feature/container) |
| 9 | **Test + format + final build.** Update `SpringLmsConsumerApplicationTests` with `spring.rabbitmq.listener.simple.auto-startup=false` so `contextLoads` passes without infra; `./mvnw spotless:apply`; final `./mvnw clean package -DskipTests` green. | `src/test/.../SpringLmsConsumerApplicationTests.java` | Build green; `spotless:check` clean |

**Manual end-to-end (after Task 9):**
1. `docker compose up -d rabbitmq mailpit` + Dockerized API (or local) + `bun scripts/seed.ts`.
2. Login (ADMIN), `POST /api/class/{classId}/export` → `202`.
3. RabbitMQ UI: message drained from `progress.report.export.q`, DLQ empty.
4. Mailpit UI: `summary.csv` + `detail.csv` attachments present.
5. Force failure (bad classId) → message in `progress.report.export.dlq`.

**Deferred / out of scope:** monorepo restructure (revisit only if shared source is wanted), unit
tests beyond `contextLoads`, `opencsv`, Excel (POI) output, retry exchange.

---

## 10. Caveats & practical notes (as of Task 8)

- **End-to-end is now VERIFIED on the live stack** (this supersedes the earlier "runtime
  unverified" note): API → RabbitMQ → consumer → Mailpit works. Concretely confirmed:
  strategy-B type-mapper round-trips `__TypeId__` (`com.qiraht.spring_lms.dto.ProgressReportMessage`)
  into the consumer DTO, `requestedAt` with 9-digit nanos parses fine (no JavaTimeModule needed),
  `buildReport` runs its derived/`@Query` methods against shared MySQL, the message is drained
  from `progress.report.export.q`, and an email with `summary.csv` + `detail.csv` lands in
  Mailpit. The failure path also works: a bad recipient / unavailable mail server → circuit
  breaker fallback → `basicReject` → DLQ.
- **Producer needs NO change for the happy path** — `ClassesService.exportProgress` (API repo,
  `7d298ff`) already falls back to the **current user's email** when `recipientEmail` is
  null/blank. Verified: even the Swagger `"string"` placeholder still delivers to the requester's
  email. Optional API-side hardening (separate repo, not done here): `@NotBlank @Email` on
  `ExportRequestDTO.recipientEmail` so a bad literal address is rejected 400 instead of reaching
  SMTP 553 → DLQ. See `docs/development-plan.md` caveats.
- **`DefaultJacksonJavaTypeMapper` (Jackson 3) over `DefaultJackson2JavaTypeMapper`.** Verified
  against the Spring AMQP 4.1.0 jars: the Jackson-3 converter is `JacksonJsonMessageConverter`
  and its correct mapper is `DefaultJacksonJavaTypeMapper` (the `Jackson2*` pair belongs to the
  deprecated Jackson-2 `Jackson2JsonMessageConverter`). The original doc's pairing was wrong and
  has been fixed in §3.
- **Mailpit runs in the main `docker-compose.yaml`** (single stack: consumer + `rabbitmq-lms` +
  `mailpit-lms`), enforcing SMTP auth `admin`/`admin` with `MP_SMTP_AUTH_ALLOW_INSECURE=true`
  (plaintext, no TLS on localhost). Web UI (8025) is open by default. The consumer's
  `spring.mail.smtp.auth` defaults to `false` — run with `SMTP_USERNAME=admin SMTP_PASSWORD=admin
  SMTP_AUTH=true` (or set them in `.env`) or mail fails. The earlier standalone
  `mailpit-docker-compose.yaml` was folded into the main compose.
- **No `RabbitTemplate`.** The consumer only listens; the publish-side client is unused. Boot
  auto-provides one lazily if a future publish requirement appears.
- **Lean repositories (deliberate).** Only aggregation-needed methods were copied (`f146520`);
  paged components, `existsBy*`, `findByAssignmentId(pageable)`, and
  `countCompletedAssignmentsByUserIdAndClassId` were omitted. Re-add only if a new feature needs
  them.
- **`DetailRow.studentName` added** beyond the spec's field list — detail rows span all students
  in one CSV, so the name column is required to make rows meaningful (approved).
- **`@Transactional(readOnly = true)` on `buildReport`.** Needed to keep the Hibernate session
  open for lazy `enrollment.getUser()`/`assignment.getClasses()` reads; performs no writes, so
  the "consumer writes nothing" contract holds.
- **Missing class → `IllegalArgumentException`.** `ProgressReportService.buildReport` throws for an
  unknown `classId`; the Task 7 listener's fallback will turn it into a reject → DLQ (the spec's
  "bad classId" verification path).
- **`buildReport` is a single read-only transaction per message**; per-student it issues ~6
  queries (same shape as the API's `ProgressService`). Acceptable for exports; revisit if volume
  grows.
- **Containerization verified.** `mvn clean package -DskipTests` builds the jar, `docker compose
  config` is valid, and `docker compose up` brings up the stack (consumer + rabbitmq + mailpit).
  The consumer container reaches the host's shared MySQL via `DB_HOST=host.docker.internal` +
  `extra_hosts: host-gateway`.
- **`application.yaml` is reconciled to spec §6** (datasource URL with
  `allowPublicKeyRetrieval`/`serverTimezone`, `ddl-auto: none`, manual ack +
  `default-requeue-rejected: false`, `mail`, `resilience4j` `report-export`) and committed with
  Task 8 (`20d62db`). The user-set `admin`/`admin` SMTP defaults were kept (they match Mailpit).
- **`mvn spring-boot:run` clean-exit — RESOLVED.** Without webmvc the app only stays alive when
  a non-daemon thread exists. With the Task 7 `@RabbitListener`, the listener container provides
  it, so `./mvnw spring-boot:run` now stays up. `@EnableRabbit` need not be added manually — Boot
  auto-registers it via `RabbitAnnotationDrivenConfiguration`.
- **Git hygiene.** `docs/` is still **untracked** in this repo (including these very edits) —
  per the decision, docs live with the main app. All consumer code (Tasks 0–8) is committed:
  Tasks 0–7 on `develop`, Task 8 (containerization) on `feature/container`. Task 9 (test +
  final `spotless:apply` build) is the only remaining item.
