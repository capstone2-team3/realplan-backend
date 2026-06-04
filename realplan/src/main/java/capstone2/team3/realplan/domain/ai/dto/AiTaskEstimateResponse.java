package capstone2.team3.realplan.domain.ai.dto;

public record AiTaskEstimateResponse(
        double aiEstimatedMinutes,
        double correctionFactor,
        double logCorrection,
        String stage
) {
}
