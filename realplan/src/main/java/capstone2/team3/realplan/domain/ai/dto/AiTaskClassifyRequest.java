package capstone2.team3.realplan.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiTaskClassifyRequest(
        String name,
        String memo,
        @JsonProperty("user_history")
        List<HistoryItem> userHistory
) {
    public record HistoryItem(
            String name,
            @JsonProperty("task_type")
            String taskType
    ) {
    }
}
