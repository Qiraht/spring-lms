--liquibase formatted sql
--changeset author:Thariq Aulia Akbar

ALTER TABLE materials
ADD COLUMN user_id BINARY(16) NOT NULL;

ALTER TABLE materials
ADD CONSTRAINT fk_materials_user_id
FOREIGN KEY (user_id) REFERENCES users(id);
