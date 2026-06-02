package capstone2.team3.realplan.domain.session.entity;

import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_task_type_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_profile_user_type",
                columnNames = {"user_id", "task_type_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserTaskTypeProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    @Column(name = "sample_count", nullable = false)
    @Builder.Default
    private int sampleCount = 0;

    @Column(name = "sum_planned_minutes", nullable = false)
    @Builder.Default
    private int sumPlannedMinutes = 0;

    @Column(name = "sum_actual_minutes", nullable = false)
    @Builder.Default
    private int sumActualMinutes = 0;

    // 오차율 = actual/planned - 1
    @Column(name = "error_ratio", precision = 6, scale = 4)
    private BigDecimal errorRatio;

    // 보정 계수 = actual/planned
    @Column(name = "bias_correction_factor", precision = 6, scale = 4)
    private BigDecimal biasCorrectionFactor;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void addSample(int plannedMinutes, int actualMinutes) {
        this.sampleCount++;
        this.sumPlannedMinutes += plannedMinutes;
        this.sumActualMinutes += actualMinutes;
        recalculate();
    }

    private void recalculate() {
        if (sumPlannedMinutes == 0) return;
        BigDecimal planned = BigDecimal.valueOf(sumPlannedMinutes);
        BigDecimal actual = BigDecimal.valueOf(sumActualMinutes);
        this.biasCorrectionFactor = actual.divide(planned, 4, java.math.RoundingMode.HALF_UP);
        this.errorRatio = this.biasCorrectionFactor.subtract(BigDecimal.ONE);
        this.lastCalculatedAt = LocalDateTime.now();
    }
}