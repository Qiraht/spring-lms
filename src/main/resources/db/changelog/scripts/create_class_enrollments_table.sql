--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS class_enrollments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    class_id VARCHAR(50) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role ENUM('TEACHER', 'STUDENT') NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL,
    deleted_at TIMESTAMP(3) NULL,
    FOREIGN KEY (class_id) references classes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) references users(id) ON DELETE CASCADE
)