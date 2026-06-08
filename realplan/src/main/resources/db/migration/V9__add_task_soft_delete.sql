ALTER TABLE task
    ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_task_user_deleted_created
    ON task (user_id, deleted_at, created_at DESC);
