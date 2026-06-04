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
@Table(name = "ai_estimation_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AiEstimationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estimation_id")
    private Long estimationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "ai_estimated_minutes", nullable = false, precision = 10, scale = 2)
    private BigDecimal aiEstimatedMinutes;

    @Column(name = "correction_factor", nullable = false, precision = 10, scale = 6)
    private BigDecimal correctionFactor;

    @Column(name = "log_correction", nullable = false, precision = 10, scale = 6)
    private BigDecimal logCorrection;

    @Column(nullable = false, length = 50)
    private String stage;

    @Column(name = "input_snapshot", nullable = false, columnDefinition = "TEXT")
    private String inputSnapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
