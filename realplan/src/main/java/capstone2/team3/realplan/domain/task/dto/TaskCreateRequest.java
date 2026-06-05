package capstone2.team3.realplan.domain.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "태스크 생성 요청")
public class TaskCreateRequest {

    @Schema(example = "1")
    @NotNull(message = "폴더 ID는 필수입니다.")
    private Long folderId;

    @Schema(example = "2")
    @NotNull(message = "태스크 유형 ID는 필수입니다.")
    private Long taskTypeId;

    @Schema(example = "OS 챕터 7 문제 풀기")
    @NotBlank(message = "태스크 이름은 필수입니다.")
    @Size(max = 255, message = "태스크 이름은 255자 이하여야 합니다.")
    private String name;

    @Schema(example = "문제집 p.142~p.180")
    private String description;

    @Schema(example = "2026-06-10T23:59:00")
    @NotNull(message = "마감일은 필수입니다.")
    private LocalDateTime dueDate;

    @Schema(example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
    @NotNull(message = "중요도는 필수입니다.")
    private String importance;    // HIGH, MEDIUM, LOW

    @Schema(example = "MEDIUM", allowableValues = {"HIGH", "MEDIUM", "LOW", "UNKNOWN"})
    @NotNull(message = "난이도는 필수입니다.")
    private String difficulty;    // HIGH, MEDIUM, LOW, UNKNOWN

    @Schema(example = "true")
    private boolean correctionEnabled = true;

    @Schema(example = "90")
    @NotNull(message = "예상 소요 시간은 필수입니다.")
    @Min(value = 1, message = "예상 소요 시간은 1분 이상이어야 합니다.")
    private Integer userEstimated;
}
