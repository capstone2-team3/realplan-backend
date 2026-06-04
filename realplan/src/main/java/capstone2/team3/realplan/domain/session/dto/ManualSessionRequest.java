package capstone2.team3.realplan.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수동 학습 기록 추가 요청
 * 타이머 없이 직접 시간을 입력해서 기록
 */
@Getter
@NoArgsConstructor
@Schema(description = "수동 학습 기록 추가 요청")
public class ManualSessionRequest {

    @Schema(example = "1")
    @NotNull(message = "태스크 ID는 필수입니다.")
    private Long taskId;

    @Schema(example = "2026-06-05T09:00:00")
    @NotNull(message = "시작 시각은 필수입니다.")
    private LocalDateTime startedAt;

    @Schema(example = "2026-06-05T10:00:00")
    @NotNull(message = "종료 시각은 필수입니다.")
    private LocalDateTime endedAt;

    @Schema(example = "3", description = "진행 속도 1~5")
    @NotNull(message = "진행 속도는 필수입니다.")
    @Min(1) @Max(5)
    private Integer progressLevel;

    @Schema(example = "45", description = "세션 후 누적 진행률 %")
    @NotNull(message = "진행률은 필수입니다.")
    @Min(0) @Max(100)
    private Integer progressPercentAfter;

    @Schema(example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH", "VERY_HIGH"})
    @NotNull(message = "집중도는 필수입니다.")
    private String focusLevel;

    @Schema(example = "도서관에서 공부")
    private String note;
}