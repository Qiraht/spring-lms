# Demo Credentials

Seeded by `scripts/seed.sql` (run via `bun scripts/seed.ts`). All demo users share one
password: `Passw0rd!` (min 8 chars, stored as bcrypt `$2b$12$`, verified by Spring's
`BCryptPasswordEncoder`).

| Email | Name | Role | Class role |
| ----- | ---- | ---- | ---------- |
| `admin@demo.com` | Demo Admin | **ADMIN** (bypasses all checks) | — |
| `teacher.java@demo.com` | Joko Wijaya | USER | TEACHER of **Java Programming** |
| `teacher.boot@demo.com` | Sari Utami | USER | TEACHER of **Spring Boot Microservices** |
| `student1@demo.com` | Budi Santoso | USER | STUDENT of Java Programming |
| `student2@demo.com` | Dewi Lestari | USER | STUDENT of Java Programming |
| `student3@demo.com` | Andi Pratama | USER | STUDENT of both classes |
| `student4@demo.com` | Rina Melati | USER | STUDENT of Spring Boot Microservices |
| `student5@demo.com` | Eko Nugroho | USER | STUDENT of Spring Boot Microservices |

> Note: user-level role is only `ADMIN`/`USER`. Being a "teacher" is an **enrollment** role
> (`class_enrollments.role = TEACHER`), which is what the resource-authorization checks use.

## Demo flows

- **Login** — `POST /api/auth/login` `{ "email": "...", "password": "Passw0rd!" }` → access + refresh token.
- **Class export (RabbitMQ)** — as ADMIN or the class teacher:
  `POST /api/class/{classId}/export` → `202` → message in `progress.report.export.q`
  (RabbitMQ UI: http://localhost:15672, guest/guest).
- **Progress summary** — as ADMIN/teacher: `GET /api/class/{classId}/summary`.
- **Audit trail** — as ADMIN: `GET /api/audit-logs` (filters + pageable).

## Re-seed

```bash
bun scripts/seed.ts
```

Idempotent — deletes the demo users/classes first, then re-creates them. Or run the raw SQL:

```bash
docker exec -i mysql mysql -u root -p<DB_PASSWORD> lmsdb < scripts/seed.sql
```
