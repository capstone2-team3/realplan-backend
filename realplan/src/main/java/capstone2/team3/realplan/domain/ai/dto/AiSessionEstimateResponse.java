package capstone2.team3.realplan.domain.ai.dto;

public record AiSessionEstimateResponse(
        double progressBasedRemainingMinutes,
        double normalizedRemainingMinutes,
        double blendingWeight,
        double finalRemainingMinutes,
        double updatedAiTotalMinutes,
        double focusWeight
) {
}
