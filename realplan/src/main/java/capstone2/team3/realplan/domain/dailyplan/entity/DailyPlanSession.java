package capstone2.team3.realplan.domain.dailyplan.entity;

import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * DailyPlanSession
 *
 * 역할: Python AI /tasks/decompose 결과를 저장하는 테이블
 *
 * DailyPlanTask가 "오늘 계획에 포함된 태스크"라면,
 * DailyPlanSession은 그 태스크를 몇 개의 실행 세션으로 쪼갰는지를 나타냄.
 *
 * 예) 120분짜리 Task → session_order=1(60분), session_order=2(60분) 2개 행 생성
 *
 * status 흐름:
 * PLANNED → SCHEDULED (자동배치 성공)
 *         → PARTIAL   (일부만 배치)
 *         → UNSCHEDULED (배치 실패)
 * SCHEDULED → IN_PROGRESS (세션 시작)
 *           → DONE        (세션 완료)
 *           → SKIPPED     (건너뜀)
 */
@Entity
@Table(
        name = "daily_plan_session",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_session_plan_task_order",
                columnNames = {"daily_plan_task_id", "session_order"}
        ),
        indexes = {
                @Index(name = "idx_dps_plan_task", columnList = "daily_plan_task_id"),
                @Index(name = "idx_dps_user",      columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyPlanSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_plan_session_id")
    private Long dailyPlanSessionId;

    // 소속 플랜 태스크 (삭제 시 함께 삭제)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_task_id", nullable = false)
    private DailyPlanTask dailyPlanTask;

    // 조회 편의용 중복 저장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // 조회 편의용 중복 저장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 같은 daily_plan_task_id 내 순서 (1부터)
    @Column(name = "session_order", nullable = false)
    private int sessionOrder;

    // AI가 제안한 raw 세션 길이 (분)
    @Column(name = "session_minutes", nullable = false)
    private int sessionMinutes;

    // 자동배치 집중도 매칭에 사용하는 요구 집중도
    @Enumerated(EnumType.STRING)
    @Column(name = "required_focus_level", nullable = false, length = 10)
    private RequiredFocusLevel requiredFocusLevel;

    // 생성 출처
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    @Builder.Default
    private SourceType sourceType = SourceType.AI;

    // 세션 계획 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private SessionPlanStatus status = SessionPlanStatus.PLANNED;

    // 미배치 또는 부분 배치 사유 (배치 성공 시 NULL)
    @Column(name = "unscheduled_reason", length = 50)
    private String unscheduledReason;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void markScheduled() {
        this.status = SessionPlanStatus.SCHEDULED;
        this.unscheduledReason = null;
    }

    public void markPartial(String reason) {
        this.status = SessionPlanStatus.PARTIAL;
        this.unscheduledReason = reason;
    }

    public void markUnscheduled(String reason) {
        this.status = SessionPlanStatus.UNSCHEDULED;
        this.unscheduledReason = reason;
    }

    public void markInProgress() {
        this.status = SessionPlanStatus.IN_PROGRESS;
    }

    public void markDone() {
        this.status = SessionPlanStatus.DONE;
    }

    public void markSkipped() {
        this.status = SessionPlanStatus.SKIPPED;
    }

    // ── Enum ─────────────────────────────────────────

    public enum RequiredFocusLevel {
        HIGH, MEDIUM, LOW, FLEXIBLE
    }

    public enum SourceType {
        AI, USER, BOTH
    }

    public enum SessionPlanStatus {
        PLANNED, SCHEDULED, PARTIAL, UNSCHEDULED, IN_PROGRESS, DONE, SKIPPED
    }
}