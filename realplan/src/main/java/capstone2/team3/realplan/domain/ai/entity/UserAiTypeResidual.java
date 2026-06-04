package capstone2.team3.realplan.domain.ai.entity;

import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "user_ai_type_residual",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_ai_type_residual",
                columnNames = {"user_id", "task_type_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserAiTypeResidual extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "residual_id")
    private Long residualId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    @Column(nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal residual = BigDecimal.ZERO;

    @Column(name = "sample_count", nullable = false)
    @Builder.Default
    private int sampleCount = 0;

    public void update(double residual, int sampleCount) {
        this.residual = BigDecimal.valueOf(residual);
        this.sampleCount = sampleCount;
    }
}
