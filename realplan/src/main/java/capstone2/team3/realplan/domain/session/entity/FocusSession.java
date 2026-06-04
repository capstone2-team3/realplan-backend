package capstone2.team3.realplan.domain.session.entity;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanTask;
import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanSession;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * FocusSession
 *
 * 역할: 집중 세션 1회 기록
 *
 * source:
 *   SESSION = 타이머로 시작한 세션
 *   MANUAL  = 수동으로 입력한 기록
 *
 * sessionStatus 흐름:
 *   ACTIVE → PAUSED → ACTIVE (재개)
 *          → ENDED
 *          → ABANDONED (강제 종료)
 *
 * plannedMinutes: 세션 시작 시점의 계획 시간
 *   플랜에서 시작하면 DailyPlanTask.plannedMinutes
 *   즉석 시작이면 null
 *
 * aiRemainingBefore: 세션 시작 전 AI 기준 잔여시간
 *   Python /sessions/estimate 호출 시 previousAiTotalMinutes 기준값으로 사용
 */
@Entity
@Table(
        name = "focus_session",
        indexes = {
                @Index(name = "idx_session_task_status", columnList = "task_id, session_status"),
                @Index(name = "idx_session_user_started", columnList = "user_id, started_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // NULLABLE — 플랜 없이 즉석 시작 허용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_task_id")
    private DailyPlanTask dailyPlanTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_session_id")
    private DailyPlanSession dailyPlanSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SessionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 10)
    @Builder.Default
    private SessionStatus sessionStatus = SessionStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    // 세션에 계획된 시간 (플랜에서 시작 시 DailyPlanTask.plannedMinutes)
    @Column(name = "planned_minutes")
    private Integer plannedMinutes;

    // 세션 시작 전 AI 기준 잔여시간 (Python /sessions/estimate 기준값)
    @Column(name = "ai_remaining_before")
    private Integer aiRemainingBefore;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void pause() {
        this.sessionStatus = SessionStatus.PAUSED;
    }

    public void resume() {
        this.sessionStatus = SessionStatus.ACTIVE;
    }

    public void end(int actualMinutes) {
        this.sessionStatus = SessionStatus.ENDED;
        this.endedAt = LocalDateTime.now();
        this.actualMinutes = actualMinutes;
    }

    public void abandon() {
        this.sessionStatus = SessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
    }

    // ── Enum ─────────────────────────────────────────

    public enum SessionSource {
        SESSION, MANUAL
    }

    public enum SessionStatus {
        ACTIVE, PAUSED, ENDED, ABANDONED
    }
}
