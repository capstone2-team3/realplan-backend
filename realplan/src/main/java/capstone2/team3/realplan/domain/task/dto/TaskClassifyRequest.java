package capstone2.team3.realplan.domain.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "태스크 유형 분류 요청")
public record TaskClassifyRequest(
        @Schema(example = "운영체제 Chap.3 정리")
        @NotBlank(message = "태스크 이름은 필수입니다.")
        @Size(max = 255, message = "태스크 이름은 255자 이하여야 합니다.")
        String name,

        @Schema(description = "선택 입력값. 없으면 현재 사용자의 기존 태스크 이력을 사용합니다.")
        @JsonProperty("user_history")
        List<@Valid HistoryItem> userHistory
) {
    public record HistoryItem(
            @NotBlank(message = "과거 태스크 이름은 필수입니다.")
            String name,

            @JsonProperty("task_type")
            @NotNull(message = "과거 태스크 유형은 필수입니다.")
            String taskType
    ) {
    }
}
