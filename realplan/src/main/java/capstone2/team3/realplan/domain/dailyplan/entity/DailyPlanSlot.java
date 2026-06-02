package capstone2.team3.realplan.domain.dailyplan.entity;

import capstone2.team3.realplan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_plan_slot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_slot_plan_index",
                columnNames = {"daily_plan_id", "slot_index"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyPlanSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id", nullable = false)
    private DailyPlan dailyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 0~41 (06:00~27:00, 30분 단위)
    // index = (hour - 6) * 2 + (minute / 30)
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}