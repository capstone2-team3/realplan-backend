package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "가용시간 슬롯 교체 요청")
public class SlotUpdateRequest {

    @Schema(example = "[6, 7, 8, 9, 14, 15, 16, 17]")
    @NotNull(message = "슬롯 목록은 필수입니다.")
    private List<Integer> slotIndexes;
}