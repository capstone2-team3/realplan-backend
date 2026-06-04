package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "데일리 플랜 생성 요청")
public class DailyPlanCreateRequest {

    @Schema(example = "2026-06-05")
    @NotNull(message = "날짜는 필수입니다.")
    private LocalDate planDate;

    // 슬롯 인덱스 목록 (0~41, 06:00~27:00 30분 단위)
    // index = (hour - 6) * 2 + (minute / 30)
    // 예) 09:00 → 6, 09:30 → 7, 13:00 → 14
    @Schema(example = "[6, 7, 8, 9, 14, 15]")
    private List<Integer> slotIndexes;
}