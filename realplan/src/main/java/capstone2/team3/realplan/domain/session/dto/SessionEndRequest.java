package capstone2.team3.realplan.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 종료 + 피드백 요청
 * 세션 종료와 동시에 피드백을 저장
 * AI /sessions/estimate 연동 시 progressPercentAfter 기반으로 잔여시간 재계산
 */
@Getter
@NoArgsConstructor
@Schema(description = "세션 종료 + 피드백 요청")
public class SessionEndRequest {

    @Schema(example = "3", description = "진행 속도 1(매우 더딤) ~ 5(매우 앞서감)")
    @NotNull(message = "진행 속도는 필수입니다.")
    @Min(1) @Max(5)
    private Integer progressLevel;

    @Schema(example = "45", description = "세션 후 누적 진행률 % (역행 가능)")
    @NotNull(message = "진행률은 필수입니다.")
    @Min(0) @Max(100)
    private Integer progressPercentAfter;

    @Schema(example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "VERY_HIGH"})
    @NotNull(message = "집중도는 필수입니다.")
    private String focusLevel;

    @Schema(example = "집중이 잘 됐어요")
    private String note;
}