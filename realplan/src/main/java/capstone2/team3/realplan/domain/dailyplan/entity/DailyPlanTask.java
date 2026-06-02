package capstone2.team3.realplan.domain.dailyplan.entity;

import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_plan_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyPlanTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_plan_task_id")
    private Long dailyPlanTaskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id", nullable = false)
    private DailyPlan dailyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    private SourceType sourceType;

    @Column(name = "planned_minutes", nullable = false)
    private int plannedMinutes;

    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private boolean isSelected = false;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void select() {
        this.isSelected = true;
    }

    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }

    // ── Enum ─────────────────────────────────────────

    public enum SourceType {
        AI, USER, BOTH
    }
}