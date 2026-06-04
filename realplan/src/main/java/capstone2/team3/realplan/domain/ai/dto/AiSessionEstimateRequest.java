package capstone2.team3.realplan.domain.ai.dto;

public record AiSessionEstimateRequest(
        double elapsedMinutes,
        double progress,
        String focusLevel,
        double previousAiTotalMinutes
) {
}
