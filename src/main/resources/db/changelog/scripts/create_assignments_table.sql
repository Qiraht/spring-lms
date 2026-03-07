--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS assignments(
    id VARCHAR(50) PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    attachment TEXT NULL,
    due_date TIMESTAMP(3) NOT NULL,
    class_id VARCHAR(50) ,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL,
    deleted_at TIMESTAMP(3) NULL,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
)