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

    /**
     * 이 플랜에서 배정된 시간(분)
     * 슬롯 배정 후 자동 계산: 배정된 슬롯 수 × 30
     */
    @Column(name = "planned_minutes", nullable = false)
    @Builder.Default
    private int plannedMinutes = 0;

    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private boolean isSelected = false;

    // ── 비즈니스 메서드 ──────────────────────────────

    public void select() { this.isSelected = true; }

    public void deselect() { this.isSelected = false; }

    public void updateDisplayOrder(int order) { this.displayOrder = order; }

    /** 슬롯 배정 변경 후 자동 재계산용 */
    public void updatePlannedMinutes(int minutes) { this.plannedMinutes = minutes; }

    // ── Enum ─────────────────────────────────────────

    public enum SourceType {
        AI, USER, BOTH
    }
}