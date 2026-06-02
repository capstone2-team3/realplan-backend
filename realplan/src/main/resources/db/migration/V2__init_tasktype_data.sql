-- ================================================================
-- V2__init_tasktype_data.sql
-- TaskType 초기 데이터 삽입
-- ================================================================

INSERT INTO task_type (code, name_ko, description, is_active, created_at, updated_at)
VALUES
    ('TIME_BASED',
     '시간형',
     '완료 시점이 명확한 작업 (예: 강의 1시간 시청)',
     TRUE,
     NOW(), NOW()),

    ('QUANTITY_BASED',
     '분량형',
     '완료 시점은 불명확하지만 완료 기준은 명확 (예: 문제 30개 풀기)',
     TRUE,
     NOW(), NOW()),

    ('SATISFACTION_BASED',
     '만족형',
     '완료 시점·완료 기준 모두 불명확 (예: 보고서 초안 작성)',
     TRUE,
     NOW(), NOW());