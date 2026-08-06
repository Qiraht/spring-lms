--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS audit_logs
(
    id BINARY(16) NOT NULL PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    user_id BINARY(16) NOT NULL,
    entity_type VARCHAR (50) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    action VARCHAR (50) NOT NULL,
    status VARCHAR (10) NOT NULL DEFAULT 'success',
    before_state JSON NULL,
    after_state JSON NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_audit_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id)
)
