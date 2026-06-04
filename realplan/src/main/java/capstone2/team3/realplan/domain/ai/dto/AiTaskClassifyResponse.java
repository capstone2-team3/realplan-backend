package capstone2.team3.realplan.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiTaskClassifyResponse(
        @JsonProperty("task_type")
        String taskType,
        String reason,
        String source
) {
}
