CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE task_type (
    task_type_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name_ko VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE folder (
    folder_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_folder_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_folder_user_name UNIQUE (user_id, name)
);

CREATE TABLE task (
    task_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    task_type_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    importance VARCHAR(10) NOT NULL,
    status VARCHAR(15) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    correction_enabled BOOLEAN NOT NULL,
    user_estimated INTEGER,
    ai_estimated INTEGER,
    final_estimated INTEGER,
    remaining_min INTEGER,
    progress_percent INTEGER NOT NULL,
    total_time INTEGER NOT NULL,
    completed_at TIMESTAMP,
    last_notified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_task_folder FOREIGN KEY (folder_id) REFERENCES folder (folder_id),
    CONSTRAINT fk_task_task_type FOREIGN KEY (task_type_id) REFERENCES task_type (task_type_id)
);

CREATE INDEX idx_task_user_due ON task (user_id, due_date);
CREATE INDEX idx_task_user_status ON task (user_id, status);

CREATE TABLE daily_plan (
    daily_plan_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_date DATE NOT NULL,
    available_minutes INTEGER NOT NULL,
    total_minutes INTEGER NOT NULL,
    status VARCHAR(15) NOT NULL,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_daily_plan_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_dailyplan_user_date UNIQUE (user_id, plan_date)
);

CREATE TABLE daily_plan_task (
    daily_plan_task_id BIGSERIAL PRIMARY KEY,
    daily_plan_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    source_type VARCHAR(10) NOT NULL,
    planned_minutes INTEGER NOT NULL,
    is_selected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_daily_plan_task_daily_plan FOREIGN KEY (daily_plan_id) REFERENCES daily_plan (daily_plan_id),
    CONSTRAINT fk_daily_plan_task_task FOREIGN KEY (task_id) REFERENCES task (task_id)
);

CREATE TABLE daily_plan_slot (
    slot_id BIGSERIAL PRIMARY KEY,
    daily_plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    slot_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_daily_plan_slot_daily_plan FOREIGN KEY (daily_plan_id) REFERENCES daily_plan (daily_plan_id),
    CONSTRAINT fk_daily_plan_slot_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_slot_plan_index UNIQUE (daily_plan_id, slot_index)
);

CREATE TABLE focus_session (
    session_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    daily_plan_task_id BIGINT,
    source VARCHAR(10) NOT NULL,
    session_status VARCHAR(10) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    actual_minutes INTEGER,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_focus_session_task FOREIGN KEY (task_id) REFERENCES task (task_id),
    CONSTRAINT fk_focus_session_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_focus_session_daily_plan_task FOREIGN KEY (daily_plan_task_id) REFERENCES daily_plan_task (daily_plan_task_id)
);

CREATE INDEX idx_session_task_status ON focus_session (task_id, session_status);
CREATE INDEX idx_session_user_started ON focus_session (user_id, started_at);

CREATE TABLE session_feedback (
    feedback_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL UNIQUE,
    progress_level INTEGER NOT NULL,
    progress_percent_after INTEGER,
    focus_level VARCHAR(10) NOT NULL,
    ai_remaining_before INTEGER,
    ai_remaining_after INTEGER,
    note TEXT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_session_feedback_session FOREIGN KEY (session_id) REFERENCES focus_session (session_id)
);

CREATE TABLE session_pause_event (
    pause_event_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    paused_at TIMESTAMP NOT NULL,
    resumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_session_pause_event_session FOREIGN KEY (session_id) REFERENCES focus_session (session_id)
);

CREATE TABLE user_task_type_profile (
    profile_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type_id BIGINT NOT NULL,
    sample_count INTEGER NOT NULL,
    sum_planned_minutes INTEGER NOT NULL,
    sum_actual_minutes INTEGER NOT NULL,
    error_ratio NUMERIC(6, 4),
    bias_correction_factor NUMERIC(6, 4),
    last_calculated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_task_type_profile_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_task_type_profile_task_type FOREIGN KEY (task_type_id) REFERENCES task_type (task_type_id),
    CONSTRAINT uq_profile_user_type UNIQUE (user_id, task_type_id)
);
