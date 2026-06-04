package capstone2.team3.realplan.domain.ai.dto;

import java.util.Map;

public record AiTaskEstimateRequest(
        int estimatedMinutes,
        int completedCount,
        String taskType,
        String difficulty,
        String folderId,
        Double userGlobal,
        Map<String, Double> userTypeResidual,
        Map<String, Double> userDifficultyResidual,
        Map<String, Double> userFolderResidual,
        Map<String, Integer> typeCount,
        Map<String, Integer> difficultyCount,
        Map<String, Integer> folderCount,
        double systemGlobalPrior,
        Map<String, Double> systemTypeEffect,
        Map<String, Double> systemDifficultyEffect
) {
}
