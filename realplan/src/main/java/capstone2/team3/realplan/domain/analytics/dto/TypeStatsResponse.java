package capstone2.team3.realplan.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TypeStatsResponse {

    private List<TypeStatsItem> types;

    @Getter
    @AllArgsConstructor
    public static class TypeStatsItem {
        private Long taskTypeId;
        private String taskTypeCode;
        private String taskTypeName;
        private int sampleCount;
        private int plannedMinutes;
        private int actualMinutes;
        private BigDecimal errorRatio;
        private BigDecimal biasCorrectionFactor;
        private BigDecimal userGlobal;
        private BigDecimal residual;
        private BigDecimal finalCorrectionFactor;
        private BigDecimal finalCorrectionPercent;
        private LocalDateTime lastCalculatedAt;
    }
}
