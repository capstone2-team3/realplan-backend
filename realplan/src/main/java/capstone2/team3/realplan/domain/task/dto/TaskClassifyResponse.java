package capstone2.team3.realplan.domain.task.dto;

import capstone2.team3.realplan.domain.ai.dto.AiTaskClassifyResponse;
import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "태스크 유형 분류 응답")
public record TaskClassifyResponse(
        Long taskTypeId,
        @JsonProperty("task_type")
        String taskType,
        String taskTypeNameKo,
        String reason,
        String source
) {
    public static TaskClassifyResponse of(AiTaskClassifyResponse response, TaskType taskType) {
        return new TaskClassifyResponse(
                taskType.getTaskTypeId(),
                response.taskType(),
                taskType.getNameKo(),
                response.reason(),
                response.source()
        );
    }
}
