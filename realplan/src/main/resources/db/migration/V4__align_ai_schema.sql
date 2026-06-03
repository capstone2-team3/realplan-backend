-- AI prediction and auto-placement schema alignment.

-- 1. Extend existing tables with AI-specific fields.
ALTER TABLE task
    ADD COLUMN IF NOT EXISTS last_ai_estimated_at TIMESTAMP(6);

ALTER TABLE focus_session
    ADD COLUMN IF NOT EXISTS planned_minutes INTEGER;

ALTER TABLE focus_session
    ADD COLUMN IF NOT EXISTS ai_remaining_before INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN IF NOT EXISTS previous_ai_total_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN IF NOT EXISTS updated_ai_total_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN IF NOT EXISTS progress_based_remaining_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN IF NOT EXISTS normalized_remaining_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN IF NOT EXISTS blending_weight NUMERIC(10, 6);

ALTER TABLE session_feedback
    ADD COLUMN IF NOT EXISTS focus_weight NUMERIC(10, 6);

-- 2. User-specific AI correction coefficient tables.
CREATE TABLE IF NOT EXISTS user_ai_profile (
    profile_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_global NUMERIC(10, 6) NOT NULL DEFAULT 0,
    completed_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_ai_profile_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_ai_profile_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS user_ai_type_residual (
    residual_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type_id BIGINT NOT NULL,
    residual NUMERIC(10, 6) NOT NULL DEFAULT 0,
    sample_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_ai_type_residual_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_ai_type_residual_task_type
        FOREIGN KEY (task_type_id) REFERENCES task_type (task_type_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_ai_type_residual UNIQUE (user_id, task_type_id)
);

CREATE TABLE IF NOT EXISTS user_ai_difficulty_residual (
    residual_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    residual NUMERIC(10, 6) NOT NULL DEFAULT 0,
    sample_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_ai_difficulty_residual_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_ai_difficulty_residual UNIQUE (user_id, difficulty)
);

ALTER TABLE folder
    ADD CONSTRAINT uq_folder_user_id_folder_id UNIQUE (user_id, folder_id);

CREATE TABLE IF NOT EXISTS user_ai_folder_residual (
    residual_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    residual NUMERIC(10, 6) NOT NULL DEFAULT 0,
    sample_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_ai_folder_residual_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_ai_folder_residual_folder_owner
        FOREIGN KEY (user_id, folder_id) REFERENCES folder (user_id, folder_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_ai_folder_residual UNIQUE (user_id, folder_id)
);

-- 3. System prior and prediction/update history.
CREATE TABLE IF NOT EXISTS ai_system_prior (
    prior_id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    system_global_prior NUMERIC(10, 6) NOT NULL DEFAULT 0,
    system_type_effect JSONB NOT NULL DEFAULT '{}'::JSONB,
    system_difficulty_effect JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_system_prior_version UNIQUE (version)
);

INSERT INTO ai_system_prior (
    version,
    is_active,
    system_global_prior,
    system_type_effect,
    system_difficulty_effect,
    created_at,
    updated_at
)
SELECT
    'v1',
    TRUE,
    0,
    '{"TIME_BASED": 0, "QUANTITY_BASED": 0, "SATISFACTION_BASED": 0}'::JSONB,
    '{"LOW": 0, "MEDIUM": 0, "HIGH": 0, "UNKNOWN": 0}'::JSONB,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_system_prior WHERE version = 'v1');

CREATE TABLE IF NOT EXISTS ai_prediction_log (
    prediction_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    estimated_minutes INTEGER NOT NULL,
    predicted_minutes NUMERIC(10, 2) NOT NULL,
    correction_factor NUMERIC(10, 6) NOT NULL,
    log_correction NUMERIC(10, 6) NOT NULL,
    stage VARCHAR(50) NOT NULL,
    input_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_prediction_log_task
        FOREIGN KEY (task_id) REFERENCES task (task_id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_prediction_log_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_coefficient_update_log (
    update_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    estimated_minutes INTEGER NOT NULL,
    actual_minutes INTEGER NOT NULL,
    planning_error_ratio NUMERIC(10, 6) NOT NULL,
    clamped_planning_error_ratio NUMERIC(10, 6) NOT NULL,
    log_ratio NUMERIC(10, 6) NOT NULL,
    clamped_log_ratio NUMERIC(10, 6) NOT NULL,
    stage VARCHAR(50) NOT NULL,
    dropped BOOLEAN NOT NULL DEFAULT FALSE,
    drop_reason VARCHAR(255),
    before_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB,
    after_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_coefficient_update_log_task
        FOREIGN KEY (task_id) REFERENCES task (task_id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_coefficient_update_log_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- 4. Lookup indexes.
CREATE INDEX IF NOT EXISTS idx_ai_system_prior_active
    ON ai_system_prior (is_active);

CREATE INDEX IF NOT EXISTS idx_ai_prediction_log_task_created_at
    ON ai_prediction_log (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_prediction_log_user_created_at
    ON ai_prediction_log (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_coefficient_update_log_task_created_at
    ON ai_coefficient_update_log (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_coefficient_update_log_user_created_at
    ON ai_coefficient_update_log (user_id, created_at DESC);
