package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "AI 자동 배치 요청")
public class DailyPlanAutoAssignRequest {

    @Schema(example = "[101, 203]", description = "자동 배치할 태스크 ID 목록. 비우면 미완료 태스크 중 우선순위 상위 태스크 사용")
    private List<Long> taskIds;

    @Schema(example = "4", description = "taskIds가 없을 때 자동 선택할 최대 태스크 수")
    @Min(value = 1, message = "maxTasks는 1 이상이어야 합니다.")
    private Integer maxTasks;

    @Schema(example = "90", description = "연속 배치 참고값. fallback에서는 응답 세션 분할 기준으로 사용")
    @Min(value = 30, message = "maxContinuousSchedulableMinutes는 30 이상이어야 합니다.")
    private Integer maxContinuousSchedulableMinutes;
}
