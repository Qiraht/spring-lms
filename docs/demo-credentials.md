# Demo Credentials

Seeded by `scripts/seed.sql` (run via `bun scripts/seed.ts`). All demo users share one
password: `Passw0rd!` (min 8 chars, stored as bcrypt `$2b$12$`, verified by Spring's
`BCryptPasswordEncoder`). The seed creates **17 users**, **6 classes**, **30 enrollments**,
**26 materials**, and **16 assignments**.

| Email | Name | Role | Class role |
| ----- | ---- | ---- | ---------- |
| `admin@demo.com` | Demo Admin | **ADMIN** (bypasses all checks) | — |
| `teacher.java@demo.com` | Joko Wijaya | USER | TEACHER of **Java Programming** |
| `teacher.boot@demo.com` | Sari Utami | USER | TEACHER of **Spring Boot Microservices** |
| `teacher.sql@demo.com` | Rudi Hartono | USER | TEACHER of **Database & SQL** |
| `teacher.frontend@demo.com` | Putri Maharani | USER | TEACHER of **Frontend Development** |
| `teacher.devops@demo.com` | Bima Prakoso | USER | TEACHER of **DevOps & CI/CD** |
| `teacher.testing@demo.com` | Dian Kusuma | USER | TEACHER of **Software Testing** |
| `student1@demo.com` | Budi Santoso | USER | STUDENT of Java Programming, Database & SQL, DevOps & CI/CD |
| `student2@demo.com` | Dewi Lestari | USER | STUDENT of Java Programming, Database & SQL, Frontend Development |
| `student3@demo.com` | Andi Pratama | USER | STUDENT of Java Programming, Spring Boot Microservices, Frontend Development, DevOps & CI/CD |
| `student4@demo.com` | Rina Melati | USER | STUDENT of Spring Boot Microservices, Database & SQL, Frontend Development |
| `student5@demo.com` | Eko Nugroho | USER | STUDENT of Spring Boot Microservices, Database & SQL, DevOps & CI/CD |
| `student6@demo.com` | Galih Purnomo | USER | STUDENT of Database & SQL |
| `student7@demo.com` | Indah Permata | USER | STUDENT of Frontend Development |
| `student8@demo.com` | Fitri Handayani | USER | STUDENT of DevOps & CI/CD, Software Testing |
| `student9@demo.com` | Rizky Ramadhan | USER | STUDENT of Database & SQL, Software Testing |
| `student10@demo.com` | Nadia Safitri | USER | STUDENT of Frontend Development, Software Testing |

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
