package capstone2.team3.realplan.domain.dailyplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 슬롯에 태스크 배정/해제 요청 DTO
 * PATCH /api/daily-plans/{planId}/slots/{slotId}
 *
 * taskId = null → 배정 해제
 * taskId = 1    → 해당 태스크 배정
 */
@Getter
@NoArgsConstructor
@Schema(description = "슬롯 태스크 배정 요청")
public class SlotAssignRequest {

    @Schema(example = "1", description = "배정할 원본 태스크 ID. null이면 배정 해제")
    private Long taskId;
}
