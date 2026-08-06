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

**Message payload:**
```java
public record ProgressReportMessage(
    String classId,        // UUID as string
    String recipientEmail, // teacher/admin who requested the export
    LocalDateTime requestedAt,
    String requesterName) {}
```

Declare the topology with `@Bean` methods (idempotent) in `RabbitMQConfig` and configure a
`Jackson2JsonMessageConverter` on the `RabbitTemplate` and `SimpleRabbitListenerContainerFactory`.

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
