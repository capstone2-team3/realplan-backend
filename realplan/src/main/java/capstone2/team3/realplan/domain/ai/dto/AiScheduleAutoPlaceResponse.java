package capstone2.team3.realplan.domain.ai.dto;

import java.util.List;

public record AiScheduleAutoPlaceResponse(
        List<ScheduleBlock> scheduleBlocks,
        List<UnscheduledSession> unscheduledSessions,
        Summary summary
) {
    public record ScheduleBlock(
            Long dailyPlanSessionId,
            Long taskId,
            List<Integer> slotIndexes
    ) {
    }

    public record UnscheduledSession(
            Long dailyPlanSessionId,
            Long taskId,
            int unscheduledMinutes,
            String reasonCode
    ) {
    }

    public record Summary(
            int scheduledMinutes,
            int unscheduledMinutes,
            int totalSchedulableMinutes,
            int slotUnitMinutes
    ) {
    }
}
