-- ================================================================
-- V6__add_slot_task_assignment.sql
-- 슬롯에 태스크 직접 배정 기능 추가
-- ================================================================

-- daily_plan_slot에 태스크 연결 컬럼 추가
-- NULL = 가용시간이지만 태스크 미배정
-- ON DELETE SET NULL = 태스크/플랜태스크 삭제 시 슬롯은 유지
ALTER TABLE daily_plan_slot
    ADD COLUMN daily_plan_task_id BIGINT
        REFERENCES daily_plan_task(daily_plan_task_id) ON DELETE SET NULL;

-- 슬롯-태스크 조회 최적화 인덱스
CREATE INDEX idx_slot_plan_task ON daily_plan_slot (daily_plan_task_id);
