package capstone2.team3.realplan.domain.ai.entity;

import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "ai_system_prior",
        uniqueConstraints = @UniqueConstraint(name = "uq_ai_system_prior_version", columnNames = "version")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AiSystemPrior extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prior_id")
    private Long priorId;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = false;

    @Column(name = "system_global_prior", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal systemGlobalPrior = BigDecimal.ZERO;

    @Column(name = "system_type_effect", nullable = false, columnDefinition = "TEXT")
    private String systemTypeEffect;

    @Column(name = "system_difficulty_effect", nullable = false, columnDefinition = "TEXT")
    private String systemDifficultyEffect;
}
