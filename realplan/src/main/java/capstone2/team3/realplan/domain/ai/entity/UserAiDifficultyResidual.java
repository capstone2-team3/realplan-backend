package capstone2.team3.realplan.domain.ai.entity;

import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "user_ai_difficulty_residual",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_ai_difficulty_residual",
                columnNames = {"user_id", "difficulty"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserAiDifficultyResidual extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "residual_id")
    private Long residualId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Task.Difficulty difficulty;

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
