-- =====================================================================
-- Spring LMS demo seed (idempotent, self-contained)
--
-- Creates: 8 demo users (password: Passw0rd!), 2 classes, 8 enrollments,
-- 6 materials, 4 assignments, 12 submissions, 30 student-progress rows.
--
-- The bcrypt hash below is for password `Passw0rd!` (cost 12, $2b$).
-- Spring's BCryptPasswordEncoder verifies $2a/$2b/$2y hashes, so these
-- users can log in normally.  `scripts/seed.ts` regenerates this hash at
-- runtime so each run gets a fresh salt.
--
-- Run it either way:
--   bun scripts/seed.ts
--   docker exec -i mysql mysql -u root -p<DB_PASSWORD> lmsdb < scripts/seed.sql
--
-- Env overrides for the bun runner:
--   DB_PASSWORD  (default supersecretpassword)
--   DB_NAME      (default lmsdb)
--   MYSQL_CONTAINER (default mysql)
--   SEED_PASSWORD (default Passw0rd!)
-- =====================================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 0. Cleanup (FK-safe order) — makes the seed re-runnable
-- ---------------------------------------------------------------------
SET @demo_emails = 'admin@demo.com,teacher.java@demo.com,teacher.boot@demo.com,'
  'student1@demo.com,student2@demo.com,student3@demo.com,student4@demo.com,student5@demo.com';

DELETE al FROM audit_logs al
  JOIN users u ON u.id = al.user_id
  WHERE FIND_IN_SET(u.email, @demo_emails);

DELETE sp FROM student_progress sp
  LEFT JOIN materials m ON m.id = sp.material_id
  LEFT JOIN assignments a ON a.id = sp.assignment_id
  LEFT JOIN classes cm ON cm.id = m.class_id
  LEFT JOIN classes ca ON ca.id = a.class_id
  WHERE COALESCE(cm.name, ca.name) IN ('Java Programming', 'Spring Boot Microservices');

DELETE sub FROM assignment_submissions sub
  JOIN assignments a ON a.id = sub.assignment_id
  JOIN classes c ON c.id = a.class_id
  WHERE c.name IN ('Java Programming', 'Spring Boot Microservices');

DELETE FROM assignments
  WHERE class_id IN (SELECT id FROM classes WHERE name IN ('Java Programming', 'Spring Boot Microservices'));

DELETE FROM materials
  WHERE class_id IN (SELECT id FROM classes WHERE name IN ('Java Programming', 'Spring Boot Microservices'));

DELETE FROM class_enrollments
  WHERE class_id IN (SELECT id FROM classes WHERE name IN ('Java Programming', 'Spring Boot Microservices'));

DELETE FROM classes
  WHERE name IN ('Java Programming', 'Spring Boot Microservices');

DELETE FROM users WHERE FIND_IN_SET(email, @demo_emails);

-- ---------------------------------------------------------------------
-- 1. Users (password: Passw0rd!)
-- ---------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, email, password, role, created_at, updated_at) VALUES
  (UUID_TO_BIN(UUID()), 'Demo', 'Admin',   'admin@demo.com',         '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'ADMIN', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY),
  (UUID_TO_BIN(UUID()), 'Joko', 'Wijaya',  'teacher.java@demo.com',  '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY),
  (UUID_TO_BIN(UUID()), 'Sari', 'Utami',   'teacher.boot@demo.com',  '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY),
  (UUID_TO_BIN(UUID()), 'Budi', 'Santoso', 'student1@demo.com',      '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
  (UUID_TO_BIN(UUID()), 'Dewi', 'Lestari', 'student2@demo.com',      '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
  (UUID_TO_BIN(UUID()), 'Andi', 'Pratama', 'student3@demo.com',      '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
  (UUID_TO_BIN(UUID()), 'Rina', 'Melati',  'student4@demo.com',      '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
  (UUID_TO_BIN(UUID()), 'Eko', 'Nugroho',  'student5@demo.com',      '$2b$12$1RHAYB/vu.RtDeR3qZd8XuR4qu3pX1CpsFq3aTzI9cL8D/FqrM8wS', 'USER',  NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY);

SET @t1 = (SELECT id FROM users WHERE email = 'teacher.java@demo.com');
SET @t2 = (SELECT id FROM users WHERE email = 'teacher.boot@demo.com');
SET @s1 = (SELECT id FROM users WHERE email = 'student1@demo.com');
SET @s2 = (SELECT id FROM users WHERE email = 'student2@demo.com');
SET @s3 = (SELECT id FROM users WHERE email = 'student3@demo.com');
SET @s4 = (SELECT id FROM users WHERE email = 'student4@demo.com');
SET @s5 = (SELECT id FROM users WHERE email = 'student5@demo.com');
SET @c1 = UUID_TO_BIN(UUID());
SET @c2 = UUID_TO_BIN(UUID());

-- ---------------------------------------------------------------------
-- 2. Classes
-- ---------------------------------------------------------------------
INSERT INTO classes (id, name, description, created_at, updated_at) VALUES
  (@c1, 'Java Programming',            'Core Java: syntax, OOP, collections, JVM basics.',     NOW() - INTERVAL 21 DAY, NOW() - INTERVAL 21 DAY),
  (@c2, 'Spring Boot Microservices',   'REST APIs, Spring Boot, RabbitMQ messaging.',          NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY);

-- ---------------------------------------------------------------------
-- 3. Enrollments (8)
-- ---------------------------------------------------------------------
INSERT INTO class_enrollments (id, class_id, user_id, role, created_at, updated_at) VALUES
  (UUID_TO_BIN(UUID()), @c1, @t1, 'TEACHER', NOW() - INTERVAL 21 DAY, NOW() - INTERVAL 21 DAY),
  (UUID_TO_BIN(UUID()), @c1, @s1, 'STUDENT', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY),
  (UUID_TO_BIN(UUID()), @c1, @s2, 'STUDENT', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY),
  (UUID_TO_BIN(UUID()), @c1, @s3, 'STUDENT', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY),
  (UUID_TO_BIN(UUID()), @c2, @t2, 'TEACHER', NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY),
  (UUID_TO_BIN(UUID()), @c2, @s3, 'STUDENT', NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY),
  (UUID_TO_BIN(UUID()), @c2, @s4, 'STUDENT', NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY),
  (UUID_TO_BIN(UUID()), @c2, @s5, 'STUDENT', NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY);

-- ---------------------------------------------------------------------
-- 4. Materials (6), creator = teacher
-- ---------------------------------------------------------------------
SET @m1 = UUID_TO_BIN(UUID());
SET @m2 = UUID_TO_BIN(UUID());
SET @m3 = UUID_TO_BIN(UUID());
SET @m4 = UUID_TO_BIN(UUID());
SET @m5 = UUID_TO_BIN(UUID());
SET @m6 = UUID_TO_BIN(UUID());

INSERT INTO materials (id, title, content, attachment, class_id, created_at, updated_at, user_id) VALUES
  (@m1, 'Java Basics',             'Variables, data types, control flow, methods.',             NULL, @c1, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY, @t1),
  (@m2, 'OOP Concepts',            'Classes, inheritance, polymorphism, interfaces.',           NULL, @c1, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, @t1),
  (@m3, 'Collections Framework',   'List, Set, Map and streams.',                               NULL, @c1, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY, @t1),
  (@m4, 'Spring Boot Intro',       'Project setup, auto-configuration, application context.',   NULL, @c2, NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY, @t2),
  (@m5, 'REST APIs',               'Controllers, DTOs, validation, error handling.',            NULL, @c2, NOW() - INTERVAL  8 DAY, NOW() - INTERVAL  8 DAY, @t2),
  (@m6, 'RabbitMQ & Messaging',    'Exchanges, queues, DLQs, circuit breaker patterns.',        NULL, @c2, NOW() - INTERVAL  4 DAY, NOW() - INTERVAL  4 DAY, @t2);

-- ---------------------------------------------------------------------
-- 5. Assignments (4), creator = teacher, future due dates
-- ---------------------------------------------------------------------
SET @a1 = UUID_TO_BIN(UUID());
SET @a2 = UUID_TO_BIN(UUID());
SET @a3 = UUID_TO_BIN(UUID());
SET @a4 = UUID_TO_BIN(UUID());

INSERT INTO assignments (id, title, content, attachment, due_date, class_id, created_at, updated_at, user_id) VALUES
  (@a1, 'Java Assignment 1: Calculator',       'Implement a console calculator with OOP.',            NULL, NOW() + INTERVAL  7 DAY, @c1, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, @t1),
  (@a2, 'Java Assignment 2: Collections Lab',  'Write a stream-based report generator.',               NULL, NOW() + INTERVAL 14 DAY, @c1, NOW() - INTERVAL  7 DAY, NOW() - INTERVAL  7 DAY, @t1),
  (@a3, 'Boot Assignment 1: REST API',         'Build a CRUD REST API with validation.',               NULL, NOW() + INTERVAL 10 DAY, @c2, NOW() - INTERVAL  9 DAY, NOW() - INTERVAL  9 DAY, @t2),
  (@a4, 'Boot Assignment 2: Messaging',        'Publish and consume a RabbitMQ message.',              NULL, NOW() + INTERVAL 17 DAY, @c2, NOW() - INTERVAL  5 DAY, NOW() - INTERVAL  5 DAY, @t2);

-- ---------------------------------------------------------------------
-- 6. Submissions (12): every student submits every assignment in class
-- ---------------------------------------------------------------------
INSERT INTO assignment_submissions (id, assignment_id, user_id, attachment, score, created_at, updated_at) VALUES
  (UUID_TO_BIN(UUID()), @a1, @s1, 'calc-s1-v1.java',    88.5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
  (UUID_TO_BIN(UUID()), @a1, @s2, 'calc-s2-v1.java',    92.0, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
  (UUID_TO_BIN(UUID()), @a1, @s3, 'calc-s3-v1.java',    79.5, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
  (UUID_TO_BIN(UUID()), @a2, @s1, 'stream-s1.java',     95.0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
  (UUID_TO_BIN(UUID()), @a2, @s2, 'stream-s2.java',     81.0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
  (UUID_TO_BIN(UUID()), @a2, @s3, 'stream-s3.java',     74.0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
  (UUID_TO_BIN(UUID()), @a3, @s3, 'rest-s3.java',       90.5, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
  (UUID_TO_BIN(UUID()), @a3, @s4, 'rest-s4.java',       85.0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
  (UUID_TO_BIN(UUID()), @a3, @s5, 'rest-s5.java',       93.5, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
  (UUID_TO_BIN(UUID()), @a4, @s3, 'mq-s3.java',         88.0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
  (UUID_TO_BIN(UUID()), @a4, @s4, 'mq-s4.java',         76.5, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
  (UUID_TO_BIN(UUID()), @a4, @s5, 'mq-s5.java',         97.0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);

-- ---------------------------------------------------------------------
-- 7. Student progress (30): each student completes every material +
--    assignment in their class (respects UNIQUE user/material + user/assignment)
-- ---------------------------------------------------------------------
INSERT INTO student_progress (id, material_id, assignment_id, user_id, is_completed, created_at, updated_at) VALUES
  -- class 1: s1
  (UUID_TO_BIN(UUID()), @m1, NULL, @s1, 1, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY),
  (UUID_TO_BIN(UUID()), @m2, NULL, @s1, 1, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY),
  (UUID_TO_BIN(UUID()), @m3, NULL, @s1, 1, NOW() - INTERVAL  9 DAY, NOW() - INTERVAL  9 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a1, @s1, 1, NOW() - INTERVAL  3 DAY, NOW() - INTERVAL  3 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a2, @s1, 1, NOW() - INTERVAL  1 DAY, NOW() - INTERVAL  1 DAY),
  -- class 1: s2
  (UUID_TO_BIN(UUID()), @m1, NULL, @s2, 1, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY),
  (UUID_TO_BIN(UUID()), @m2, NULL, @s2, 1, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY),
  (UUID_TO_BIN(UUID()), @m3, NULL, @s2, 1, NOW() - INTERVAL  9 DAY, NOW() - INTERVAL  9 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a1, @s2, 1, NOW() - INTERVAL  3 DAY, NOW() - INTERVAL  3 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a2, @s2, 1, NOW() - INTERVAL  1 DAY, NOW() - INTERVAL  1 DAY),
  -- class 1: s3
  (UUID_TO_BIN(UUID()), @m1, NULL, @s3, 1, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY),
  (UUID_TO_BIN(UUID()), @m2, NULL, @s3, 1, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY),
  (UUID_TO_BIN(UUID()), @m3, NULL, @s3, 1, NOW() - INTERVAL  9 DAY, NOW() - INTERVAL  9 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a1, @s3, 1, NOW() - INTERVAL  3 DAY, NOW() - INTERVAL  3 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a2, @s3, 1, NOW() - INTERVAL  1 DAY, NOW() - INTERVAL  1 DAY),
  -- class 2: s3
  (UUID_TO_BIN(UUID()), @m4, NULL, @s3, 1, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY),
  (UUID_TO_BIN(UUID()), @m5, NULL, @s3, 1, NOW() - INTERVAL  7 DAY, NOW() - INTERVAL  7 DAY),
  (UUID_TO_BIN(UUID()), @m6, NULL, @s3, 1, NOW() - INTERVAL  3 DAY, NOW() - INTERVAL  3 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a3, @s3, 1, NOW() - INTERVAL  2 DAY, NOW() - INTERVAL  2 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a4, @s3, 1, NOW() - INTERVAL  1 DAY, NOW() - INTERVAL  1 DAY),
  -- class 2: s4
  (UUID_TO_BIN(UUID()), @m4, NULL, @s4, 1, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY),
  (UUID_TO_BIN(UUID()), @m5, NULL, @s4, 1, NOW() - INTERVAL  7 DAY, NOW() - INTERVAL  7 DAY),
  (UUID_TO_BIN(UUID()), @m6, NULL, @s4, 1, NOW() - INTERVAL  3 DAY, NOW() - INTERVAL  3 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a3, @s4, 1, NOW() - INTERVAL  2 DAY, NOW() - INTERVAL  2 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a4, @s4, 1, NOW() - INTERVAL  1 DAY, NOW() - INTERVAL  1 DAY),
  -- class 2: s5
  (UUID_TO_BIN(UUID()), @m4, NULL, @s5, 1, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY),
  (UUID_TO_BIN(UUID()), @m5, NULL, @s5, 1, NOW() - INTERVAL  7 DAY, NOW() - INTERVAL  7 DAY),
  (UUID_TO_BIN(UUID()), @m6, NULL, @s5, 1, NOW() - INTERVAL  3 DAY, NOW() - INTERVAL  3 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a3, @s5, 1, NOW() - INTERVAL  2 DAY, NOW() - INTERVAL  2 DAY),
  (UUID_TO_BIN(UUID()), NULL, @a4, @s5, 1, NOW() - INTERVAL  1 DAY, NOW() - INTERVAL  1 DAY);

COMMIT;

-- ---------------------------------------------------------------------
-- Summary
-- ---------------------------------------------------------------------
SELECT '--- seeded users ---' AS info;
SELECT email, role FROM users WHERE email LIKE '%@demo.com' ORDER BY role, email;

SELECT '--- classes (use these ids for the export demo) ---' AS info;
SELECT BIN_TO_UUID(id) AS class_id, name FROM classes ORDER BY created_at;

SELECT '--- table counts ---' AS info;
SELECT
  (SELECT COUNT(*) FROM users)                  AS users,
  (SELECT COUNT(*) FROM classes)                AS classes,
  (SELECT COUNT(*) FROM class_enrollments)      AS enrollments,
  (SELECT COUNT(*) FROM materials)              AS materials,
  (SELECT COUNT(*) FROM assignments)            AS assignments,
  (SELECT COUNT(*) FROM assignment_submissions) AS submissions,
  (SELECT COUNT(*) FROM student_progress)       AS progress;
