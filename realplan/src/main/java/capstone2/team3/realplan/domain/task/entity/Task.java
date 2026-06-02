package capstone2.team3.realplan.domain.task.entity;

import capstone2.team3.realplan.domain.folder.entity.Folder;
import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "task",
        indexes = {
                @Index(name = "idx_task_user_due", columnList = "user_id, due_date"),
                @Index(name = "idx_task_user_status", columnList = "user_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    // 프런트 Task.name 에 대응
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // 프런트 Task.importance 에 대응
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Importance importance = Importance.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    // 프런트 Task.correctionEnabled 에 대응
    @Column(name = "correction_enabled", nullable = false)
    @Builder.Default
    private boolean correctionEnabled = true;

    // 프런트 Task.originalEstimatedMin 에 대응
    @Column(name = "user_estimated")
    private Integer userEstimated;

    // 프런트 Task.adjustedEstimatedMin 에 대응 (AI 보정 후)
    @Column(name = "ai_estimated")
    private Integer aiEstimated;

    // 최종 사용 예상 시간 (보정 미사용 시 userEstimated, 사용 시 aiEstimated)
    @Column(name = "final_estimated")
    private Integer finalEstimated;

    // 프런트 Task.remainingMin 에 대응 — 세션 종료 시 서비스 레이어에서 차감
    @Column(name = "remaining_min")
    private Integer remainingMin;

    // 프런트 Task.progressPercent 에 대응 (역행 가능)
    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private int progressPercent = 0;

    // 누적 실제 소요 시간
    @Column(name = "total_time", nullable = false)
    @Builder.Default
    private int totalTime = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 프런트 Task.lastNotifiedAt 에 대응
    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void complete() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercent = 100;
        this.remainingMin = 0;
    }

    public void updateProgress(int progressPercent, int addedMinutes) {
        this.progressPercent = progressPercent;
        this.totalTime += addedMinutes;
        if (this.remainingMin != null) {
            this.remainingMin = Math.max(0, this.remainingMin - addedMinutes);
        }
        if (this.status == Status.PENDING) {
            this.status = Status.IN_PROGRESS;
        }
    }

    public void updateFolder(Folder folder) {
        this.folder = folder;
    }

    public void updateLastNotifiedAt(LocalDateTime notifiedAt) {
        this.lastNotifiedAt = notifiedAt;
    }

    public void updateAiEstimated(int aiEstimated) {
        this.aiEstimated = aiEstimated;
        if (this.correctionEnabled) {
            this.finalEstimated = aiEstimated;
            this.remainingMin = aiEstimated - this.totalTime;
        }
    }

    // TaskService의 부분 업데이트용 (null이면 기존 값 유지)
    public void updateInfo(String name, String description, LocalDateTime dueDate,
                           Importance importance, Difficulty difficulty,
                           Boolean correctionEnabled, Integer userEstimated) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (dueDate != null) this.dueDate = dueDate;
        if (importance != null) this.importance = importance;
        if (difficulty != null) this.difficulty = difficulty;
        if (correctionEnabled != null) this.correctionEnabled = correctionEnabled;
        if (userEstimated != null) {
            this.userEstimated = userEstimated;
            if (!this.correctionEnabled) {
                this.finalEstimated = userEstimated;
                this.remainingMin = userEstimated - this.totalTime;
            }
        }
    }

    // ── Enum ─────────────────────────────────────────

    public enum Importance {
        LOW, MEDIUM, HIGH
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED
    }

    public enum Difficulty {
        LOW, MEDIUM, HIGH, UNKNOWN
    }
}
