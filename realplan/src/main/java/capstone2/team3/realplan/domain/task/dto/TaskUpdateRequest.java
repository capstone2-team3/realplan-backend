package capstone2.team3.realplan.domain.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "태스크 수정 요청")
public class TaskUpdateRequest {

    @Schema(example = "2")
    private Long folderId;

    @Schema(example = "OS 챕터 7 문제 풀기")
    private String name;

    @Schema(example = "문제집 p.142~p.180")
    private String description;

    @Schema(example = "2026-06-10T23:59:00")
    private LocalDateTime dueDate;

    @Schema(example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
    private String importance;

    @Schema(example = "MEDIUM", allowableValues = {"HIGH", "MEDIUM", "LOW", "UNKNOWN"})
    private String difficulty;

    @Schema(example = "true")
    private Boolean correctionEnabled;

    @Schema(example = "90")
    private Integer userEstimated;
}
