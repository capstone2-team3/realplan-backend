package capstone2.team3.realplan.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FocusByHourResponse {

    private List<FocusByHourItem> buckets;

    @Getter
    @AllArgsConstructor
    public static class FocusByHourItem {
        private int startHour;
        private int endHour;
        private String label;
        private Double averageFocus;
        private long sessionCount;
    }
}
