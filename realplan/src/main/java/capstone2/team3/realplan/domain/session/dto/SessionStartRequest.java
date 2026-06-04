package capstone2.team3.realplan.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 시작 요청
 * dailyPlanSessionId: AI 분할 세션에서 시작하면 입력
 * dailyPlanTaskId: 플랜 태스크에서 시작하면 입력, 즉석 시작이면 null
 */
@Getter
@NoArgsConstructor
@Schema(description = "세션 시작 요청")
public class SessionStartRequest {

    @Schema(example = "1")
    @NotNull(message = "태스크 ID는 필수입니다.")
    private Long taskId;

    @Schema(example = "1", description = "플랜에서 시작 시 입력. 즉석 시작이면 null")
    private Long dailyPlanTaskId;  // NULLABLE — 플랜 없이 즉석 시작 허용

    @Schema(example = "1", description = "AI 분할 세션에서 시작 시 입력. 있으면 dailyPlanTaskId보다 우선")
    private Long dailyPlanSessionId;
}
