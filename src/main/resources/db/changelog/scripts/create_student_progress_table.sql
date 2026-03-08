--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

CREATE TABLE IF NOT EXISTS student_progress (
    id BINARY(16) NOT NULL PRIMARY KEY,
    material_id VARCHAR(50) NULL,
    assignment_id VARCHAR(50) NULL,
    user_id BINARY(16) NOT NULL,
    is_completed  TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL,
    deleted_at TIMESTAMP(3) NULL,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, material_id),
    UNIQUE(user_id, assignment_id)
);