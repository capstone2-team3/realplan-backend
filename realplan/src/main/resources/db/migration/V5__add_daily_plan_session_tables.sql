-- ================================================================
-- V5__add_daily_plan_session_tables.sql
-- AI 자동배치를 위한 세션 분할/배치 결과 저장 테이블 추가
-- ================================================================

-- ── daily_plan_session ───────────────────────────────────────────
-- 역할: /tasks/decompose 결과 저장
--       DailyPlanTask 하나를 몇 개의 실행 세션으로 쪼갰는지 기록
--       예) 120분짜리 Task → 60분 세션 2개로 분할
CREATE TABLE daily_plan_session (
                                    daily_plan_session_id BIGSERIAL    PRIMARY KEY,
                                    daily_plan_task_id    BIGINT       NOT NULL
                                        REFERENCES daily_plan_task(daily_plan_task_id) ON DELETE CASCADE,
                                    task_id               BIGINT       NOT NULL REFERENCES task(task_id),
                                    user_id               BIGINT       NOT NULL REFERENCES users(user_id),
                                    session_order         INT          NOT NULL,   -- 같은 daily_plan_task 내 순서 (1부터)
                                    session_minutes       INT          NOT NULL CHECK (session_minutes > 0),
                                    required_focus_level  VARCHAR(10)  NOT NULL,   -- HIGH, MEDIUM, LOW, FLEXIBLE
                                    source_type           VARCHAR(10)  NOT NULL DEFAULT 'AI',  -- AI, USER, BOTH
                                    status                VARCHAR(15)  NOT NULL DEFAULT 'PLANNED',
    -- PLANNED: 분할만 됨
    -- SCHEDULED: 시간표 배치 완료
    -- PARTIAL: 일부만 배치됨
    -- UNSCHEDULED: 배치 실패
    -- IN_PROGRESS: 세션 진행 중
    -- DONE: 세션 완료
    -- SKIPPED: 건너뜀
                                    unscheduled_reason    VARCHAR(50),             -- 미배치 사유 (배치 성공 시 NULL)
                                    created_at            TIMESTAMP    NOT NULL,
                                    updated_at            TIMESTAMP    NOT NULL,
                                    CONSTRAINT uq_session_plan_task_order UNIQUE (daily_plan_task_id, session_order)
);

-- ── daily_plan_session_block ─────────────────────────────────────
-- 역할: /schedules/auto-place 결과 저장
--       논리 세션(daily_plan_session)이 실제 시간표의 어느 슬롯에
--       배치됐는지 30분 단위 조각으로 기록
--       예) 60분 세션 → 09:00~09:30, 09:30~10:00 두 블록으로 저장
CREATE TABLE daily_plan_session_block (
                                          session_block_id      BIGSERIAL   PRIMARY KEY,
                                          daily_plan_session_id BIGINT      NOT NULL
                                              REFERENCES daily_plan_session(daily_plan_session_id) ON DELETE CASCADE,
                                          block_order           INT         NOT NULL,    -- 같은 논리 세션 내 순서 (1부터)
                                          start_time            VARCHAR(5)  NOT NULL,    -- "HH:MM" (27:00 표현 허용)
                                          end_time              VARCHAR(5)  NOT NULL,
                                          duration_minutes      INT         NOT NULL,    -- 보통 30분 단위
                                          created_at            TIMESTAMP   NOT NULL,
                                          updated_at            TIMESTAMP   NOT NULL,
                                          CONSTRAINT uq_block_session_order UNIQUE (daily_plan_session_id, block_order)
);

-- ── focus_session 컬럼 추가 ──────────────────────────────────────
-- 역할: 세션 시작 시점의 계획 시간/AI 잔여시간/분할 세션 출처 저장
--       세션 종료 후 AI 잔여시간 재계산의 기준값으로 사용
ALTER TABLE focus_session
    ADD COLUMN daily_plan_session_id BIGINT;

ALTER TABLE focus_session
    ADD COLUMN planned_minutes INT;   -- 이 세션에 계획된 시간(분)

ALTER TABLE focus_session
    ADD COLUMN ai_remaining_before INT;   -- 세션 시작 전 AI 기준 잔여시간

ALTER TABLE focus_session
    ADD CONSTRAINT fk_focus_session_daily_plan_session
        FOREIGN KEY (daily_plan_session_id)
        REFERENCES daily_plan_session(daily_plan_session_id)
        ON DELETE SET NULL;

-- ── task 컬럼 추가 ───────────────────────────────────────────────
-- 역할: AI 예측 시간이 마지막으로 갱신된 시각 추적
--       /tasks/estimate 호출 후 갱신, 재예측 주기 판단에 사용
ALTER TABLE task
    ADD COLUMN last_ai_estimated_at TIMESTAMP;

-- ── 인덱스 ───────────────────────────────────────────────────────
CREATE INDEX idx_dps_plan_task ON daily_plan_session (daily_plan_task_id);
CREATE INDEX idx_dps_user      ON daily_plan_session (user_id);
CREATE INDEX idx_dpsb_session  ON daily_plan_session_block (daily_plan_session_id);
