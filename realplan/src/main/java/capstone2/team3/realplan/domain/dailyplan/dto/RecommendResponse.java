package capstone2.team3.realplan.domain.dailyplan.dto;

import lombok.Getter;

import java.util.List;

/**
 * AI 태스크 추천 결과 응답 DTO
 * GET /api/daily-plans/{id}/recommend 응답
 *
 * Python /tasks/recommend 응답을 그대로 프런트에 전달
 */
@Getter
public class RecommendResponse {

    private final String targetDate;
    private final int availableMinutes;
    private final List<RecommendItem> recommendations;
    private final String message;

    public RecommendResponse(String targetDate, int availableMinutes,
                             List<RecommendItem> recommendations, String message) {
        this.targetDate = targetDate;
        this.availableMinutes = availableMinutes;
        this.recommendations = recommendations;
        this.message = message;
    }

    @Getter
    public static class RecommendItem {
        private final int rank;
        private final Long taskId;
        private final String name;
        private final int remainingMin;
        private final double recommendScore;
        private final int deadlineScore;
        private final int workloadUrgencyScore;
        private final int importanceScore;
        private final boolean isDueToday;
        private final String deadlineLabel;
        private final String importanceLabel;
        private final String recommendedTimeBand;
        private final String recommendedTimeBandLabel;
        private final String requiredFocusLevel;
        private final String reason;

        public RecommendItem(int rank, Long taskId, String name, int remainingMin,
                             double recommendScore, int deadlineScore, int workloadUrgencyScore,
                             int importanceScore, boolean isDueToday, String deadlineLabel,
                             String importanceLabel, String recommendedTimeBand,
                             String recommendedTimeBandLabel, String requiredFocusLevel,
                             String reason) {
            this.rank = rank;
            this.taskId = taskId;
            this.name = name;
            this.remainingMin = remainingMin;
            this.recommendScore = recommendScore;
            this.deadlineScore = deadlineScore;
            this.workloadUrgencyScore = workloadUrgencyScore;
            this.importanceScore = importanceScore;
            this.isDueToday = isDueToday;
            this.deadlineLabel = deadlineLabel;
            this.importanceLabel = importanceLabel;
            this.recommendedTimeBand = recommendedTimeBand;
            this.recommendedTimeBandLabel = recommendedTimeBandLabel;
            this.requiredFocusLevel = requiredFocusLevel;
            this.reason = reason;
        }
    }
}