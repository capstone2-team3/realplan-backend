package capstone2.team3.realplan.domain.dailyplan.entity;

import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_plan",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dailyplan_user_date",
                columnNames = {"user_id", "plan_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_plan_id")
    private Long dailyPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "available_minutes", nullable = false)
    @Builder.Default
    private int availableMinutes = 0;

    @Column(name = "total_minutes", nullable = false)
    @Builder.Default
    private int totalMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private PlanStatus status = PlanStatus.RECOMMENDED;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void confirm() {
        this.status = PlanStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = PlanStatus.REJECTED;
    }

    public void end() {
        this.status = PlanStatus.ENDED;
    }

    public void updateAvailableMinutes(int minutes) {
        this.availableMinutes = minutes;
    }

    // ── Enum ─────────────────────────────────────────

    public enum PlanStatus {
        RECOMMENDED, CONFIRMED, ENDED, REJECTED
    }
}