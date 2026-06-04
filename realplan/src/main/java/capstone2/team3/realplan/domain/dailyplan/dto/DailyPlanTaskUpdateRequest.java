package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "플랜 내 태스크 수정 요청")
public class DailyPlanTaskUpdateRequest {

    @Schema(example = "true")
    private Boolean isSelected;

    @Schema(example = "2")
    private Integer displayOrder;
}