package capstone2.team3.realplan.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class DifficultyCorrectionResponse {

    private List<DifficultyCorrectionItem> items;

    @Getter
    @AllArgsConstructor
    public static class DifficultyCorrectionItem {
        private String difficulty;
        private String difficultyLabel;
        private int sampleCount;
        private BigDecimal residual;
        private BigDecimal correctionPercent;
        private LocalDateTime updatedAt;
    }
}
