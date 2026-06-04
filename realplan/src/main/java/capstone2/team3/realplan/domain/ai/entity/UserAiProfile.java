package capstone2.team3.realplan.domain.ai.entity;

import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "user_ai_profile",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_ai_profile_user", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserAiProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_global", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal userGlobal = BigDecimal.ZERO;

    @Column(name = "completed_count", nullable = false)
    @Builder.Default
    private int completedCount = 0;

    public void update(double userGlobal, int completedCount) {
        this.userGlobal = BigDecimal.valueOf(userGlobal);
        this.completedCount = completedCount;
    }
}
