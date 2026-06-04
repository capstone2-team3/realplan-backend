package capstone2.team3.realplan.domain.ai.dto;

import java.util.List;

public record AiTaskDecomposeRequest(
        int slotUnitMinutes,
        int maxContinuousSchedulableMinutes,
        List<TaskItem> tasks
) {
    public record TaskItem(
            Long taskId,
            String title,
            String memo,
            String taskType,
            String difficulty,
            int remainingMin,
            int activeScheduledMin
    ) {
    }
}
