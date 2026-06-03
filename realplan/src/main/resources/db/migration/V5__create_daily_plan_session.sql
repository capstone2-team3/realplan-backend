-- AI task decomposition results for daily plan sessions.

CREATE TABLE IF NOT EXISTS daily_plan_session (
    daily_plan_session_id BIGSERIAL PRIMARY KEY,
    daily_plan_task_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    session_order INTEGER NOT NULL,
    session_minutes INTEGER NOT NULL,
    required_focus_level VARCHAR(10) NOT NULL,
    source_type VARCHAR(10) NOT NULL DEFAULT 'AI',
    status VARCHAR(15) NOT NULL DEFAULT 'PLANNED',
    unscheduled_reason VARCHAR(50),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_plan_session_daily_plan_task
        FOREIGN KEY (daily_plan_task_id) REFERENCES daily_plan_task (daily_plan_task_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_daily_plan_session_task
        FOREIGN KEY (task_id) REFERENCES task (task_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_daily_plan_session_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_daily_plan_session_order
        UNIQUE (daily_plan_task_id, session_order),
    CONSTRAINT ck_daily_plan_session_minutes_positive
        CHECK (session_minutes > 0),
    CONSTRAINT ck_daily_plan_session_required_focus_level
        CHECK (required_focus_level IN ('HIGH', 'MEDIUM', 'LOW', 'FLEXIBLE')),
    CONSTRAINT ck_daily_plan_session_source_type
        CHECK (source_type IN ('AI', 'USER', 'BOTH')),
    CONSTRAINT ck_daily_plan_session_status
        CHECK (status IN ('PLANNED', 'SCHEDULED', 'PARTIAL', 'UNSCHEDULED', 'IN_PROGRESS', 'DONE', 'SKIPPED'))
);

CREATE TABLE IF NOT EXISTS daily_plan_session_block (
    session_block_id BIGSERIAL PRIMARY KEY,
    daily_plan_session_id BIGINT NOT NULL,
    block_order INTEGER NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_plan_session_block_session
        FOREIGN KEY (daily_plan_session_id)
        REFERENCES daily_plan_session (daily_plan_session_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_daily_plan_session_block_order
        UNIQUE (daily_plan_session_id, block_order),
    CONSTRAINT ck_daily_plan_session_block_duration_positive
        CHECK (duration_minutes > 0),
    CONSTRAINT ck_daily_plan_session_block_time_format
        CHECK (
            start_time ~ '^[0-9]{2}:[0-9]{2}$'
            AND end_time ~ '^[0-9]{2}:[0-9]{2}$'
        )
);

ALTER TABLE focus_session
    ADD COLUMN IF NOT EXISTS daily_plan_session_id BIGINT;

ALTER TABLE focus_session
    ADD CONSTRAINT fk_focus_session_daily_plan_session
        FOREIGN KEY (daily_plan_session_id) REFERENCES daily_plan_session (daily_plan_session_id)
        ON DELETE SET NULL;

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
