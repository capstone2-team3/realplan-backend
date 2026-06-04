package capstone2.team3.realplan.domain.ai.dto;

import java.util.List;

public record AiTaskDecomposeResponse(
        List<TaskSession> taskSessions
) {
    public record TaskSession(
            Long dailyPlanSessionId,
            Long taskId,
            int sessionMinutes,
            String requiredFocusLevel
    ) {
    }
}
