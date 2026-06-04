package capstone2.team3.realplan.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class DailyStudyTimeResponse {

    private int weeks;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DailyStudyTimeItem> days;

    @Getter
    @AllArgsConstructor
    public static class DailyStudyTimeItem {
        private LocalDate date;
        private int totalMinutes;
    }
}
