# Spring LMS API

A Learning Management System REST API built with Spring Boot 4 and Java 21. Supports
class management, student enrollment, material/assignment tracking, submission grading,
progress monitoring, audit logging, and event-driven progress report export.

## Tech Stack

| Component           | Technology                                     |
|---------------------|------------------------------------------------|
| Languag e           | Java 21                                        |
| Framework           | Spring Boot 4.1.0 / Spring Framework 7         |
| Database            | MySQL 8.0+                                     |
| Cache / Token Store | Valkey 7.2 (Redis-compatible)                  |
| Message Broker      | RabbitMQ 3                                     |
| ORM                 | Spring Data JPA / Hibernate                    |
| Migrations          | Liquibase                                      |
| Security            | Spring Security + JWT (jjwt 0.13.0)            |
| Resilience          | Resilience4j (rate limiting + circuit breaker) |
| API Docs            | Springdoc OpenAPI 3.0.2 (Swagger UI)           |
| Code Style          | Palantir Java Format (Spotless)                |
| Build               | Maven                                          |

## Features

- **Authentication & Authorization** — JWT access + refresh tokens with rotation and reuse
  detection, role-based access control (ADMIN / USER), per-request active-user validation
- **Class Management** — CRUD for classes, materials, and assignments with soft-delete
- **Enrollment** — bulk enroll/remove users from classes with teacher/student roles
- **Submissions & Grading** — assignment submissions with scoring and validation
- **Progress Tracking** — per-material completion tracking with batch-aggregated summaries
- **Audit Logging** — AOP-driven audit trail with before/after state diffs, filterable via API
- **Event-Driven Export** — progress report export via RabbitMQ with circuit breaker resilience
- **Rate Limiting** — Resilience4j rate limiting on auth endpoints (100 req/60s)
- **API Documentation** — Swagger UI available in dev profile

## Dependencies & Technology Highlights

- **Spring AOP** (`spring-boot-starter-aspectj`) — audit logging via `@Auditable` annotation
  and `AuditAspect`; captures before/after entity state automatically
- **Resilience4j** — rate limiting on login/refresh endpoints; circuit breaker on RabbitMQ
  publish with fallback to 503
- **Spring Data Redis** — refresh token storage and revocation in Valkey/Redis
- **Spring AMQP** — RabbitMQ integration for async progress report export with DLQ
- **Liquibase** — database schema migrations with versioned changesets
- **JWT (jjwt 0.13.0)** — separate access/refresh secrets, issuer verification, token
  rotation with Redis-backed reuse detection
- **Springdoc OpenAPI 3.0.2** — auto-generated Swagger UI with `@ParameterObject` for
  pageable endpoints
- **Testcontainers** — integration tests against real MySQL and Valkey containers
- **Lombok** — boilerplate reduction across entities, DTOs, and services

## Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8.0+
- Valkey 7.2+ or Redis 7+ (for refresh tokens and rate limiting)
- RabbitMQ 3+ (optional, only for progress report export)
- Docker & Docker Compose (optional, for containerized setup)

## Getting Started

### 1. Clone and configure

```bash
git clone https://github.com/qiraht/spring-lms.git
cd spring-lms
cp example.env .env
# Edit .env with your database credentials and JWT secrets
```

### 2. Run with Maven (local)

Start MySQL and Valkey manually or via Docker:

```bash
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=changeme \
  -e MYSQL_DATABASE=lmsdb \
  mysql:8.0

docker run -d --name valkey -p 6379:6379 valkey/valkey:7.2
```

Update `.env` with `DB_HOST=localhost` and `REDIS_HOST=localhost`, then:

```bash
dotenvx run -- mvn spring-boot:run
```

> **Tip:** [dotenvx](https://dotenvx.dev) loads `.env` into the process automatically.
> Install via `npm install -g @dotenvx/dotenvx` or see [dotenvx.dev](https://dotenvx.dev)
> for other install methods.

The API starts on `http://localhost:8080`. Liquibase applies pending changesets on startup.

### 3. Run with Docker Compose

```bash
cp example.env .env
# Edit .env with your credentials
docker compose up -d
```

The compose file runs the API + RabbitMQ. MySQL and Valkey are commented out as optional
— uncomment them in `docker-compose.yaml` if you don't have them running externally.

### 4. Seed demo data (optional)

```bash
# Via SQL (requires mysql CLI or Docker exec)
docker exec -i mysql mysql -u root -p<DB_PASSWORD> lmsdb < scripts/seed.sql

# Or via Bun/TypeScript
bun scripts/seed.ts
```

See `docs/demo-credentials.md` for demo user emails and passwords.

### 5. Promote an admin user

```sql
UPDATE users SET role = 'ADMIN' WHERE email = '<your-email>';
```

## API Documentation

Once the app is running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

> **Note:** Swagger is only enabled in the `dev` profile. In production, API docs are
> disabled for security.

### Key Endpoints

| Method  | Path                          | Auth          | Description                            |
|---------|-------------------------------|---------------|----------------------------------------|
| `POST`  | `/api/auth/login`             | Public        | Login, returns access + refresh tokens |
| `POST`  | `/api/auth/refresh`           | Public        | Refresh access token                   |
| `POST`  | `/api/auth/logout`            | Bearer        | Logout, revokes refresh tokens         |
| `POST`  | `/api/auth/register`          | Public        | Register a new user                    |
| `GET`   | `/api/users`                  | ADMIN         | List users                             |
| `POST`  | `/api/classes`                | ADMIN         | Create a class                         |
| `GET`   | `/api/classes`                | Authenticated | List classes                           |
| `POST`  | `/api/class/{id}/enroll`      | ADMIN/Teacher | Bulk enroll users                      |
| `GET`   | `/api/class/{id}/summary`     | ADMIN/Teacher | Student progress summaries             |
| `POST`  | `/api/class/{id}/export`      | ADMIN/Teacher | Queue progress report export           |
| `POST`  | `/api/material`               | ADMIN         | Create material                        |
| `POST`  | `/api/assignment`             | ADMIN         | Create assignment                      |
| `POST`  | `/api/submission`             | Authenticated | Submit assignment                      |
| `POST`  | `/api/submission/{id}/grade`  | ADMIN/Teacher | Grade submission                       |
| `POST`  | `/api/material/{id}/complete` | Authenticated | Mark material as completed             |
| `GET`   | `/api/audit-logs`             | ADMIN         | View audit trail (filterable)          |

> API documentation is a work in progress. See Swagger UI for full request/response schemas.

## Project Structure

```
src/main/java/com/qiraht/spring_lms/
├── annotation/       # Custom annotations (@Auditable)
├── aspect/           # AOP aspects (AuditAspect)
├── config/           # Security, CORS, RabbitMQ config
├── controller/       # REST controllers
├── dto/              # Request/Response DTOs
├── entity/           # JPA entities
├── Enum/             # Role, Action, Status enums
├── exception/        # Custom exceptions
├── handler/          # Exception handlers, auth entry points
├── repository/       # Spring Data JPA repositories
├── security/         # JWT filter, user details
├── service/          # Business logic
└── util/             # JWT utility
```

## Architecture

See [docs/architecture.md](docs/architecture.md) for the full architecture overview,
entity relationship diagram, and deployment topology.

## Related Repositories

- **spring-lms-consumer** — RabbitMQ consumer that builds CSV progress reports and emails
  them. Runs with its own Docker Compose stack (consumer + RabbitMQ + Mailpit).

## License

This project is for portfolio and educational purposes.
