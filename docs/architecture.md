# Architecture

## 1. High-Level Architecture

The system is split into two applications sharing one MySQL database:

- **spring-lms (API)** — Spring Boot 4 / Java 21, serves all REST endpoints, writes the
  audit trail, and publishes progress-report export messages.
- **spring-lms-consumer (separate repo)** — consumes export messages, builds CSV reports,
  and emails them.

```mermaid
graph TB
    subgraph Client["Client Layer"]
        FE["Frontend / API Consumer\n(Browser, Mobile, Postman)"]
    end

    subgraph API["API Application (spring-lms) - Spring Boot 4 / Java 21"]
        SEC["Security Layer\n(JWT Access + Refresh Filter, CORS)\nLogin & Refresh endpoints public"]
        CTRL["Controller Layer\nAuth, User, Classes, Enrollment, Material,\nAssignment, Submission, Progress, Audit, Export"]
        SVC["Service Layer\nBusiness Logic (Controller-Service-Repository)"]
        REPO["Repository Layer\nSpring Data JPA"]
        AUDIT["Audit Logging\n(AOP @Auditable + AuditService)"]
    end

    subgraph Infra["Supporting Infrastructure"]
        REDIS["Redis\n(Refresh Token Store + Cache)"]
        RABBIT["RabbitMQ\n(Export Queue + DLQ)"]
    end

  subgraph DB["Database"]
    MYSQL["MySQL 8.0+"]
  end

    subgraph Consumer["Consumer Application (spring-lms-consumer) - separate repo"]
        LISTENER["RabbitMQ Listener"]
        REPORT["Report Builder\n(Aggregate + Detail CSV)"]
        MAIL["Mail Service\n(JavaMailSender)"]
    end

    subgraph Support["Cross-cutting"]
        SWAGGER["Swagger / OpenAPI Docs\n(/swagger-ui, /v3/api-docs)"]
        LIQUIBASE["Liquibase\n(DB Migrations)"]
        R4J["Resilience4j\n(Rate Limiter + Circuit Breaker)"]
    end

    FE -->|"HTTPS / REST JSON"| SEC
    SEC --> CTRL
    CTRL --> SVC
    SVC --> REPO
    REPO --> MYSQL
    SVC --> AUDIT
    AUDIT --> MYSQL
    LIQUIBASE --> MYSQL
    SVC --> R4J
    SVC --> REDIS
    SVC -->|"publish export message"| RABBIT
    RABBIT -->|"consume"| LISTENER
    LISTENER --> REPORT
    REPORT --> MYSQL
    REPORT --> MAIL
    MAIL -->|"SMTP"| MAILPIT["Mailpit (dev SMTP)"]
    FE -->|"API docs"| SWAGGER
```

## 2. Deployment Architecture

The API runs as a Docker container; MySQL runs separately and is reached over the network
(connection settings via env vars). Redis runs alongside the API in its Compose project,
while RabbitMQ and Mailpit are defined in the consumer's own Compose file. GitHub Actions
builds and pushes the API image to Docker Hub on pushes to `main`. Both Compose projects
run on the same host network so the API can reach the consumer's RabbitMQ.

```mermaid
graph LR
    subgraph CI["CI (GitHub Actions)"]
        BUILD["mvn clean package\n(Java 21, Temurin)"]
        PUSH["Build & Push Image\n(Docker Hub)"]
    end
    subgraph Host["Docker Host"]
        subgraph ComposeAPI["Docker Compose (API repo)"]
            API_C["spring-lms-api\n(Java 21, :8080)"]
            REDIS_C["redis\n(6379)"]

        end
        subgraph ComposeConsumer["Docker Compose (consumer repo)"]
          RABBIT_C["rabbitmq:3-management\n(5672 / 15672)"]
          CON_C["spring-lms-consumer\n(Java 21)"]
          MAILPIT_C["mailpit\n(SMTP 1025 / UI 8025)"]
        end
    end

    MYSQL["MySQL 8.0\n(external, via DB_HOST:DB_PORT)"]

    PUSH --> API_C
    API_C -->|"JDBC (DB_USER/DB_PASSWORD/DB_NAME)"| MYSQL
    API_C --> REDIS_C
    API_C -->|"publish"| RABBIT_C
    RABBIT_C -->|"consume"| CON_C
    CON_C -->|"JDBC (read)"| MYSQL
    CON_C -->|"SMTP"| MAILPIT_C
    BUILD -.-> PUSH
```

## 3. Entity Relationship Diagram

```mermaid
erDiagram
    USERS {
        BINARY16 id PK
        varchar first_name
        varchar last_name
        text email
        text password
        varchar role
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    CLASSES {
        BINARY16 id PK
        varchar name
        text description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    MATERIALS {
        BINARY16 id PK
        BINARY16 class_id FK
        BINARY16 user_id FK
        text title
        text content
        text attachment
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    ASSIGNMENTS {
        BINARY16 id PK
        BINARY16 class_id FK
        BINARY16 user_id FK
        text title
        text content
        text attachment
        timestamp due_date
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    CLASS_ENROLLMENTS {
        BINARY16 id PK
        BINARY16 class_id FK
        BINARY16 user_id FK
        varchar role
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    ASSIGNMENT_SUBMISSIONS {
        BINARY16 id PK
        BINARY16 assignment_id FK
        BINARY16 user_id FK
        text attachment
        decimal score
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    STUDENT_PROGRESS {
        BINARY16 id PK
        BINARY16 user_id FK
        BINARY16 material_id FK
        BINARY16 assignment_id FK
        tinyint is_completed
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    AUDIT_LOGS {
        BINARY16 id PK
        BINARY16 user_id FK
        varchar entity_type
        BINARY16 entity_id
        varchar action
        varchar status
        json before_state
        json after_state
        timestamp created_at
    }

    CLASSES ||--o{ MATERIALS : "contains"
    CLASSES ||--o{ ASSIGNMENTS : "contains"
    CLASSES ||--o{ CLASS_ENROLLMENTS : "has"
    USERS ||--o{ MATERIALS : "authors"
    USERS ||--o{ ASSIGNMENTS : "creates"
    USERS ||--o{ CLASS_ENROLLMENTS : "enrolled in"
    USERS ||--o{ ASSIGNMENT_SUBMISSIONS : "submits"
    ASSIGNMENTS ||--o{ ASSIGNMENT_SUBMISSIONS : "receives"
    USERS ||--o{ STUDENT_PROGRESS : "tracks"
    MATERIALS ||--o{ STUDENT_PROGRESS : "tracks"
    ASSIGNMENTS ||--o{ STUDENT_PROGRESS : "tracks"
    USERS ||--o{ AUDIT_LOGS : "performs"
```

Note: `audit_logs.user_id` intentionally has **no `ON DELETE CASCADE`** — audit history is
preserved even when a user is deleted (unlike the other FKs which cascade).

### Audit Logs Actions
||

## 4. Feature Notes

### Audit Logs

- Generic append-only table; one row per action.
- Captured via **AOP** (`@Auditable(entityType, action, idExpr)`) for CRUD + **explicit
  calls** in `AuthService` (login / refresh / logout) and bulk operations
  (enroll / remove / progress).
- `before_state` / `after_state` are JSON snapshots of scalar entity fields only
  (associations are skipped to avoid lazy-loading issues).
- Persisted in a `REQUIRES_NEW` transaction so **failed** actions are recorded too.
- Actions: `create`, `update`, `delete`, `login`, `logout`, `refresh`, `export`.
- Viewing: `GET /api/audit-logs` and `GET /api/audit-logs/{id}` (ADMIN only, filterable).

### Auth: Access + Refresh Tokens

- Login returns `{ email, accessToken, refreshToken, expiresIn }`.
- Access token (`type=access`) is short-lived; refresh token (`type=refresh`) is long-lived
  and stored in Redis (`refresh:token:<jti>`) for revocation.
- `POST /api/auth/refresh` rotates the refresh token; `POST /api/auth/logout` revokes all
  of a user's refresh tokens (real logout for stateless JWTs).

### Event-Driven Progress Report Export

- `POST /api/class/{classId}/export` (ADMIN or teacher) publishes a `ProgressReportMessage`
  to RabbitMQ and returns `202 Queued` (audit `action=export` recorded).
- Publication is protected by a **Resilience4j circuit breaker** (`rabbitmq-publish`) with a
  fallback that surfaces a 5xx when the broker is unavailable.
- The **consumer service** (separate repo, see `docs/consumer-service.md`) builds a class
  progress report (aggregate summary + per-assignment/material detail), attaches
  `summary.csv` and `detail.csv`, emails it via Mailpit (dev) or a real SMTP, and routes
  failed messages to the dead-letter queue `progress.report.export.dlq`.

### Admin Bootstrap (manual)

No seed code. Promote a user to ADMIN via SQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = '<your-admin-email>';
```
