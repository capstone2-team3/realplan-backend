-- ================================================================
-- V7__add_ai_storage_tables.sql
-- AI coefficient, prior, and log storage
-- ================================================================

ALTER TABLE session_feedback
    ADD COLUMN previous_ai_total_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN updated_ai_total_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN progress_based_remaining_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN normalized_remaining_minutes INTEGER;

ALTER TABLE session_feedback
    ADD COLUMN blending_weight NUMERIC(10, 6);

ALTER TABLE session_feedback
    ADD COLUMN focus_weight NUMERIC(10, 6);

CREATE TABLE user_ai_profile (
    profile_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_global NUMERIC(10, 6) NOT NULL DEFAULT 0,
    completed_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_ai_profile_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uq_user_ai_profile_user UNIQUE (user_id)
);

CREATE TABLE user_ai_type_residual (
    residual_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type_id BIGINT NOT NULL,
    residual NUMERIC(10, 6) NOT NULL DEFAULT 0,
    sample_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_ai_type_residual_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_ai_type_residual_type FOREIGN KEY (task_type_id) REFERENCES task_type(task_type_id),
    CONSTRAINT uq_user_ai_type_residual UNIQUE (user_id, task_type_id)
);

CREATE TABLE user_ai_difficulty_residual (
    residual_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    residual NUMERIC(10, 6) NOT NULL DEFAULT 0,
    sample_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_ai_difficulty_residual_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uq_user_ai_difficulty_residual UNIQUE (user_id, difficulty)
);

ALTER TABLE folder
    ADD CONSTRAINT uq_folder_user_id_folder_id UNIQUE (user_id, folder_id);

CREATE TABLE user_ai_folder_residual (
    residual_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    residual NUMERIC(10, 6) NOT NULL DEFAULT 0,
    sample_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_ai_folder_residual_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_ai_folder_residual_folder FOREIGN KEY (user_id, folder_id) REFERENCES folder(user_id, folder_id),
    CONSTRAINT uq_user_ai_folder_residual UNIQUE (user_id, folder_id)
);

CREATE TABLE ai_system_prior (
    prior_id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    system_global_prior NUMERIC(10, 6) NOT NULL DEFAULT 0,
    system_type_effect TEXT NOT NULL,
    system_difficulty_effect TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
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
) VALUES (
    'v1',
    TRUE,
    0,
    '{"TIME_BASED":0,"QUANTITY_BASED":0,"SATISFACTION_BASED":0}',
    '{"LOW":0,"MEDIUM":0,"HIGH":0,"UNKNOWN":0}',
    NOW(),
    NOW()
);

CREATE TABLE ai_estimation_log (
    estimation_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    estimated_minutes INTEGER NOT NULL,
    ai_estimated_minutes NUMERIC(10, 2) NOT NULL,
    correction_factor NUMERIC(10, 6) NOT NULL,
    log_correction NUMERIC(10, 6) NOT NULL,
    stage VARCHAR(50) NOT NULL,
    input_snapshot TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ai_estimation_log_task FOREIGN KEY (task_id) REFERENCES task(task_id),
    CONSTRAINT fk_ai_estimation_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE ai_coefficient_update_log (
    update_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    estimated_minutes INTEGER NOT NULL,
    actual_minutes INTEGER NOT NULL,
    planning_error_ratio NUMERIC(10, 6),
    clamped_planning_error_ratio NUMERIC(10, 6),
    log_ratio NUMERIC(10, 6),
    clamped_log_ratio NUMERIC(10, 6),
    stage VARCHAR(50) NOT NULL,
    dropped BOOLEAN NOT NULL DEFAULT FALSE,
    drop_reason VARCHAR(255),
    before_snapshot TEXT NOT NULL,
    after_snapshot TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ai_coefficient_update_log_task FOREIGN KEY (task_id) REFERENCES task(task_id),
    CONSTRAINT fk_ai_coefficient_update_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_user_ai_type_residual_user ON user_ai_type_residual(user_id);
CREATE INDEX idx_user_ai_difficulty_residual_user ON user_ai_difficulty_residual(user_id);
CREATE INDEX idx_user_ai_folder_residual_user ON user_ai_folder_residual(user_id);
CREATE INDEX idx_ai_estimation_log_task ON ai_estimation_log(task_id);
CREATE INDEX idx_ai_coefficient_update_log_task ON ai_coefficient_update_log(task_id);
