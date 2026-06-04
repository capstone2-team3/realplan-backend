package capstone2.team3.realplan.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class WeeklyAnalyticsResponse {

    private LocalDate weekStart;
    private LocalDate weekEnd;
    private Metric<Integer> totalMinutes;
    private Metric<Double> averageFocus;
    private Metric<Long> completedTasks;

    @Getter
    @AllArgsConstructor
    public static class Metric<T> {
        private T current;
        private T previous;
        private T diff;
    }
}
