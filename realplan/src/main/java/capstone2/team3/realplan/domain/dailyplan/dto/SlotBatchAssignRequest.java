package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI auto-place 결과를 슬롯에 일괄 배정하는 요청 DTO
 * PUT /api/daily-plans/{planId}/slots/batch
 *
 * AI가 슬롯 인덱스 기반으로 결과를 주면:
 * {
 *   "scheduleBlocks": [
 *     { "taskId": 101, "slotIndexes": [6, 7, 8] }
 *   ]
 * }
 * → 슬롯 6,7,8에 Task 101 배정
 */
@Getter
@NoArgsConstructor
@Schema(description = "AI 자동배치 결과 슬롯 일괄 배정 요청")
public class SlotBatchAssignRequest {

    @NotEmpty(message = "scheduleBlocks는 필수입니다.")
    private List<@Valid ScheduleBlock> scheduleBlocks;

    @Getter
    @NoArgsConstructor
    public static class ScheduleBlock {

        @Schema(example = "101")
        @NotNull(message = "taskId는 필수입니다.")
        private Long taskId;

        @Schema(example = "[6, 7, 8]", description = "배정할 슬롯 인덱스 목록 (0~41)")
        @NotEmpty(message = "slotIndexes는 필수입니다.")
        private List<Integer> slotIndexes;
    }
}
