-- ================================================================
-- V8__harden_daily_plan_session_constraints.sql
-- Daily plan AI session tables: constraint/index hardening
-- ================================================================

ALTER TABLE daily_plan_session
    ADD CONSTRAINT ck_daily_plan_session_minutes_positive
        CHECK (session_minutes > 0);

ALTER TABLE daily_plan_session
    ADD CONSTRAINT ck_daily_plan_session_required_focus_level
        CHECK (required_focus_level IN ('HIGH', 'MEDIUM', 'LOW', 'FLEXIBLE'));

ALTER TABLE daily_plan_session
    ADD CONSTRAINT ck_daily_plan_session_source_type
        CHECK (source_type IN ('AI', 'USER', 'BOTH'));

ALTER TABLE daily_plan_session
    ADD CONSTRAINT ck_daily_plan_session_status
        CHECK (status IN ('PLANNED', 'SCHEDULED', 'PARTIAL', 'UNSCHEDULED', 'IN_PROGRESS', 'DONE', 'SKIPPED'));

ALTER TABLE daily_plan_session_block
    ADD CONSTRAINT ck_daily_plan_session_block_duration_positive
        CHECK (duration_minutes > 0);

ALTER TABLE daily_plan_session_block
    ADD CONSTRAINT ck_daily_plan_session_block_time_format
        CHECK (start_time LIKE '__:__' AND end_time LIKE '__:__');

CREATE INDEX IF NOT EXISTS idx_daily_plan_session_daily_plan_task
    ON daily_plan_session (daily_plan_task_id, session_order);

CREATE INDEX IF NOT EXISTS idx_daily_plan_session_task_status
    ON daily_plan_session (task_id, status);

CREATE INDEX IF NOT EXISTS idx_daily_plan_session_user_created_at
    ON daily_plan_session (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_daily_plan_session_block_session
    ON daily_plan_session_block (daily_plan_session_id, block_order);

CREATE INDEX IF NOT EXISTS idx_focus_session_daily_plan_session
    ON focus_session (daily_plan_session_id);
