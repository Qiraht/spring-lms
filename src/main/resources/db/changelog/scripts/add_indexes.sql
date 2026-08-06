--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

ALTER TABLE users MODIFY email VARCHAR(255) NOT NULL;
CREATE UNIQUE INDEX idx_users_email ON users(email);

CREATE INDEX idx_enrollments_class_user_role ON class_enrollments(class_id, user_id, role);
CREATE INDEX idx_submissions_assignment_user ON assignment_submissions(assignment_id, user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
