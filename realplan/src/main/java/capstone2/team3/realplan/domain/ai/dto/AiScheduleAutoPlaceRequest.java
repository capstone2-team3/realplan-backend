package capstone2.team3.realplan.domain.ai.dto;

import java.util.List;

public record AiScheduleAutoPlaceRequest(
        int slotUnitMinutes,
        Integer maxContinuousSchedulableMinutes,
        List<SchedulableTimeBlock> schedulableTimeBlocks,
        List<FocusTimeSlot> focusTimeSlots,
        List<TaskItem> tasks,
        List<TaskSession> taskSessions
) {
    public record SchedulableTimeBlock(
            String start,
            String end
    ) {
    }

    public record FocusTimeSlot(
            String start,
            String end,
            int focusScore
    ) {
    }

    public record TaskItem(
            Long taskId,
            boolean isDueToday,
            double recommendScore,
            int targetMinutes,
            String difficulty
    ) {
    }

    public record TaskSession(
            Long dailyPlanSessionId,
            Long taskId,
            int sessionMinutes,
            String requiredFocusLevel
    ) {
    }
}
