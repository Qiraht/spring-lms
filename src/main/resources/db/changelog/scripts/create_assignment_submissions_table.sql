--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    assignment_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    attachment TEXT NOT NULL,
    score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL,
    deleted_at TIMESTAMP(3) NULL,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);