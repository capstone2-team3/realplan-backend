package capstone2.team3.realplan.domain.session.entity;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanTask;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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