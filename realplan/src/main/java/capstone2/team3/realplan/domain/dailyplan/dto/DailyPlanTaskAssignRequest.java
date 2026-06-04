package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "데일리 플랜 슬롯 태스크 직접 배정 요청")
public class DailyPlanTaskAssignRequest {

    @Schema(example = "101", description = "배정할 원본 태스크 ID")
    @NotNull(message = "taskId는 필수입니다.")
    private Long taskId;

    @Schema(example = "[6, 7, 8]", description = "태스크를 배정할 슬롯 인덱스 목록 (0~41)")
    @NotEmpty(message = "slotIndexes는 필수입니다.")
    private List<Integer> slotIndexes;
}
