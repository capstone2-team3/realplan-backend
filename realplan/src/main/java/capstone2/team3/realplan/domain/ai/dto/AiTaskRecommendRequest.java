package capstone2.team3.realplan.domain.ai.dto;

import java.util.List;

public record AiTaskRecommendRequest(
        String targetDate,
        String requestedAt,
        int availableMinutes,
        List<TimeBandFocusScore> timeBandFocusScores,
        List<TaskItem> tasks
) {
    public record TimeBandFocusScore(
            String timeBand,
            int focusScore
    ) {
    }

    public record TaskItem(
            Long taskId,
            String name,
            String dueDate,
            String importance,
            String taskType,
            String difficulty,
            String status,
            int remainingMin,
            int activeScheduledMin
    ) {
    }
}
