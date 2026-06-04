package capstone2.team3.realplan.domain.dailyplan.dto;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlan;
import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanSlot;
import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanTask;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DailyPlanResponse {

    private final Long dailyPlanId;
    private final LocalDate planDate;
    private final int availableMinutes;
    private final int totalMinutes;
    private final String status;
    private final LocalDateTime confirmedAt;
    private final List<SlotResponse> slots;
    private final List<PlanTaskResponse> tasks;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private DailyPlanResponse(DailyPlan plan, List<DailyPlanSlot> slots, List<DailyPlanTask> tasks) {
        this.dailyPlanId = plan.getDailyPlanId();
        this.planDate = plan.getPlanDate();
        this.availableMinutes = plan.getAvailableMinutes();
        this.totalMinutes = plan.getTotalMinutes();
        this.status = plan.getStatus().name();
        this.confirmedAt = plan.getConfirmedAt();
        this.slots = slots.stream().map(SlotResponse::from).toList();
        this.tasks = tasks.stream().map(PlanTaskResponse::from).toList();
        this.createdAt = plan.getCreatedAt();
        this.updatedAt = plan.getUpdatedAt();
    }

    public static DailyPlanResponse of(DailyPlan plan, List<DailyPlanSlot> slots, List<DailyPlanTask> tasks) {
        return new DailyPlanResponse(plan, slots, tasks);
    }

    // ── 슬롯 응답 ──────────────────────────────────────────────────

    @Getter
    public static class SlotResponse {
        private final Long slotId;
        private final int slotIndex;
        private final String timeLabel;
        private final Long dailyPlanTaskId;  // null = 미배정
        private final Long taskId;           // null = 미배정
        private final String taskName;       // null = 미배정

        private SlotResponse(DailyPlanSlot slot) {
            this.slotId = slot.getSlotId();
            this.slotIndex = slot.getSlotIndex();
            this.timeLabel = slot.toTimeLabel();
            if (slot.getDailyPlanTask() != null) {
                this.dailyPlanTaskId = slot.getDailyPlanTask().getDailyPlanTaskId();
                this.taskId = slot.getDailyPlanTask().getTask().getTaskId();
                this.taskName = slot.getDailyPlanTask().getTask().getName();
            } else {
                this.dailyPlanTaskId = null;
                this.taskId = null;
                this.taskName = null;
            }
        }

        public static SlotResponse from(DailyPlanSlot slot) {
            return new SlotResponse(slot);
        }
    }

    // ── 플랜 태스크 응답 ───────────────────────────────────────────
    // plannedMinutes = 배정된 슬롯 수 × 30 으로 자동 계산됨

    @Getter
    public static class PlanTaskResponse {
        private final Long dailyPlanTaskId;
        private final Long taskId;
        private final String taskName;
        private final String taskTypeCode;
        private final String importance;
        private final int displayOrder;
        private final String sourceType;
        private final int plannedMinutes;  // 배정된 슬롯 수 × 30
        private final boolean isSelected;

        private PlanTaskResponse(DailyPlanTask planTask) {
            this.dailyPlanTaskId = planTask.getDailyPlanTaskId();
            this.taskId = planTask.getTask().getTaskId();
            this.taskName = planTask.getTask().getName();
            this.taskTypeCode = planTask.getTask().getTaskType().getCode();
            this.importance = planTask.getTask().getImportance().name();
            this.displayOrder = planTask.getDisplayOrder();
            this.sourceType = planTask.getSourceType().name();
            this.plannedMinutes = planTask.getPlannedMinutes();
            this.isSelected = planTask.isSelected();
        }

        public static PlanTaskResponse from(DailyPlanTask planTask) {
            return new PlanTaskResponse(planTask);
        }
    }
}