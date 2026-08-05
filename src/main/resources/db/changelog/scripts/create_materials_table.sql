--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS materials(
    id BINARY(16) NOT NULL PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    attachment TEXT NULL,
    class_id BINARY(16) ,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL,
    deleted_at TIMESTAMP(3) NULL,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
)