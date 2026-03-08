--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

ALTER TABLE assignments
ADD COLUMN user_id BINARY(16) NOT NULL;

ALTER TABLE assignments
ADD CONSTRAINT fk_assignments_user_id
FOREIGN KEY (user_id) REFERENCES users(id);