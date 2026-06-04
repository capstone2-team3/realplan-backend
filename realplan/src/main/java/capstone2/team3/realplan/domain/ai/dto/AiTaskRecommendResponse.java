package capstone2.team3.realplan.domain.ai.dto;

import java.util.List;

public record AiTaskRecommendResponse(
        String targetDate,
        int availableMinutes,
        List<RecommendationItem> recommendations,
        String message
) {
    public record RecommendationItem(
            int rank,
            Long taskId,
            String name,
            int remainingMin,
            double recommendScore,
            int deadlineScore,
            int workloadUrgencyScore,
            int importanceScore,
            boolean isDueToday,
            String deadlineLabel,
            String importanceLabel,
            String recommendedTimeBand,
            String recommendedTimeBandLabel,
            String requiredFocusLevel,
            String reason
    ) {
    }
}
