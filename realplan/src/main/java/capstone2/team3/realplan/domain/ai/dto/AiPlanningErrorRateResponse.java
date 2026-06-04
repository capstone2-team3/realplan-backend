package capstone2.team3.realplan.domain.ai.dto;

import java.util.Map;

public record AiPlanningErrorRateResponse(
        double userGlobal,
        Map<String, Double> userTypeResidual,
        Map<String, Double> userDifficultyResidual,
        Map<String, Double> userFolderResidual,
        Map<String, Integer> typeCount,
        Map<String, Integer> difficultyCount,
        Map<String, Integer> folderCount,
        double planningErrorRatio,
        double clampedPlanningErrorRatio,
        double logRatio,
        double clampedLogRatio,
        String stage,
        boolean dropped,
        String dropReason
) {
}
