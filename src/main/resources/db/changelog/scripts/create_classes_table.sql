--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS classes
(
    id VARCHAR(50) NOT NULL PRIMARY KEY,
    name VARCHAR (50) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL,
    deleted_at TIMESTAMP(3) NULL
)