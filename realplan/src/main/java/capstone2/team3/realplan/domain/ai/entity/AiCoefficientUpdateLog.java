package capstone2.team3.realplan.domain.ai.entity;

import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_coefficient_update_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AiCoefficientUpdateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "update_id")
    private Long updateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "actual_minutes", nullable = false)
    private int actualMinutes;

    @Column(name = "planning_error_ratio", precision = 10, scale = 6)
    private BigDecimal planningErrorRatio;

    @Column(name = "clamped_planning_error_ratio", precision = 10, scale = 6)
    private BigDecimal clampedPlanningErrorRatio;

    @Column(name = "log_ratio", precision = 10, scale = 6)
    private BigDecimal logRatio;

    @Column(name = "clamped_log_ratio", precision = 10, scale = 6)
    private BigDecimal clampedLogRatio;

    @Column(nullable = false, length = 50)
    private String stage;

    @Column(nullable = false)
    @Builder.Default
    private boolean dropped = false;

    @Column(name = "drop_reason", length = 255)
    private String dropReason;

    @Column(name = "before_snapshot", nullable = false, columnDefinition = "TEXT")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", nullable = false, columnDefinition = "TEXT")
    private String afterSnapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
