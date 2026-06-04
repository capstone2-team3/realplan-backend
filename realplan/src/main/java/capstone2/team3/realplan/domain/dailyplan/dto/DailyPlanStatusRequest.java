package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "플랜 상태 변경 요청")
public class DailyPlanStatusRequest {

    @Schema(example = "CONFIRMED", allowableValues = {"CONFIRMED", "REJECTED"})
    @NotNull(message = "상태는 필수입니다.")
    private String status;
}