package capstone2.team3.realplan.domain.analytics.service;

import capstone2.team3.realplan.domain.analytics.dto.DailyStudyTimeResponse;
import capstone2.team3.realplan.domain.analytics.dto.DifficultyCorrectionResponse;
import capstone2.team3.realplan.domain.analytics.dto.FocusByHourResponse;
import capstone2.team3.realplan.domain.analytics.dto.TypeStatsResponse;
import capstone2.team3.realplan.domain.analytics.dto.WeeklyAnalyticsResponse;
import capstone2.team3.realplan.domain.ai.entity.UserAiDifficultyResidual;
import capstone2.team3.realplan.domain.ai.repository.UserAiDifficultyResidualRepository;
import capstone2.team3.realplan.domain.session.entity.FocusSession;
import capstone2.team3.realplan.domain.session.entity.SessionFeedback;
import capstone2.team3.realplan.domain.session.entity.UserTaskTypeProfile;
import capstone2.team3.realplan.domain.session.repository.FocusSessionRepository;
import capstone2.team3.realplan.domain.session.repository.SessionFeedbackRepository;
import capstone2.team3.realplan.domain.session.repository.UserTaskTypeProfileRepository;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final FocusSessionRepository focusSessionRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final UserTaskTypeProfileRepository userTaskTypeProfileRepository;
    private final UserAiDifficultyResidualRepository userAiDifficultyResidualRepository;
    private final TaskRepository taskRepository;

    public WeeklyAnalyticsResponse getWeekly(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        LocalDateTime currentStart = weekStart.atStartOfDay();
        LocalDateTime currentEnd = weekStart.plusWeeks(1).atStartOfDay();
        LocalDateTime previousStart = weekStart.minusWeeks(1).atStartOfDay();
        LocalDateTime previousEnd = currentStart;

        List<FocusSession> currentSessions = findEndedSessions(userId, currentStart, currentEnd);
        List<FocusSession> previousSessions = findEndedSessions(userId, previousStart, previousEnd);

        int currentMinutes = sumActualMinutes(currentSessions);
        int previousMinutes = sumActualMinutes(previousSessions);
        Double currentFocus = averageFocus(currentSessions);
        Double previousFocus = averageFocus(previousSessions);
        long currentCompleted = taskRepository.countByUserUserIdAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                userId, currentStart, currentEnd);
        long previousCompleted = taskRepository.countByUserUserIdAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                userId, previousStart, previousEnd);

        return new WeeklyAnalyticsResponse(
                weekStart,
                weekEnd,
                new WeeklyAnalyticsResponse.Metric<>(currentMinutes, previousMinutes, currentMinutes - previousMinutes),
                new WeeklyAnalyticsResponse.Metric<>(currentFocus, previousFocus, diff(currentFocus, previousFocus)),
                new WeeklyAnalyticsResponse.Metric<>(currentCompleted, previousCompleted, currentCompleted - previousCompleted)
        );
    }

    public DailyStudyTimeResponse getDaily(Long userId, int weeks) {
        int normalizedWeeks = Math.max(1, Math.min(weeks, 12));
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays((long) normalizedWeeks * 7 - 1);

        List<FocusSession> sessions = findEndedSessions(userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        Map<LocalDate, Integer> minutesByDate = sessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.getStartedAt().toLocalDate(),
                        Collectors.summingInt(session -> valueOrZero(session.getActualMinutes()))));

        List<DailyStudyTimeResponse.DailyStudyTimeItem> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            days.add(new DailyStudyTimeResponse.DailyStudyTimeItem(date, minutesByDate.getOrDefault(date, 0)));
        }

        return new DailyStudyTimeResponse(normalizedWeeks, startDate, endDate, days);
    }

    public FocusByHourResponse getFocusByHour(Long userId) {
        LocalDateTime start = LocalDate.now().minusWeeks(4).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        List<FocusSession> sessions = findEndedSessions(userId, start, end);
        Map<Long, SessionFeedback> feedbackBySessionId = findFeedbackBySessionId(sessions);

        List<FocusByHourResponse.FocusByHourItem> buckets = new ArrayList<>();
        for (int startHour = 0; startHour < 24; startHour += 2) {
            int bucketStart = startHour;
            int bucketEnd = startHour + 2;
            List<Double> scores = sessions.stream()
                    .filter(session -> session.getStartedAt().getHour() >= bucketStart
                            && session.getStartedAt().getHour() < bucketEnd)
                    .map(session -> feedbackBySessionId.get(session.getSessionId()))
                    .filter(feedback -> feedback != null)
                    .map(feedback -> (double) focusScore(feedback.getFocusLevel()))
                    .toList();

            buckets.add(new FocusByHourResponse.FocusByHourItem(
                    bucketStart,
                    bucketEnd,
                    String.format("%02d:00-%02d:00", bucketStart, bucketEnd),
                    roundAverage(scores),
                    scores.size()));
        }

        return new FocusByHourResponse(buckets);
    }

    public TypeStatsResponse getTypeStats(Long userId) {
        List<TypeStatsResponse.TypeStatsItem> items = userTaskTypeProfileRepository.findAllByUserUserIdOrderByTaskTypeCodeAsc(userId)
                .stream()
                .map(this::toTypeStatsItem)
                .toList();
        return new TypeStatsResponse(items);
    }

    public DifficultyCorrectionResponse getDifficultyCorrection(Long userId) {
        Map<Task.Difficulty, UserAiDifficultyResidual> residualByDifficulty =
                userAiDifficultyResidualRepository.findAllByUserUserId(userId)
                        .stream()
                        .collect(Collectors.toMap(
                                UserAiDifficultyResidual::getDifficulty,
                                Function.identity(),
                                (left, right) -> left,
                                () -> new EnumMap<>(Task.Difficulty.class)));

        List<DifficultyCorrectionResponse.DifficultyCorrectionItem> items = Arrays.stream(Task.Difficulty.values())
                .map(difficulty -> toDifficultyCorrectionItem(
                        difficulty,
                        residualByDifficulty.get(difficulty)))
                .toList();
        return new DifficultyCorrectionResponse(items);
    }

    private List<FocusSession> findEndedSessions(Long userId, LocalDateTime start, LocalDateTime end) {
        return focusSessionRepository.findAllByUserUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndSessionStatus(
                userId, start, end, FocusSession.SessionStatus.ENDED);
    }

    private int sumActualMinutes(List<FocusSession> sessions) {
        return sessions.stream()
                .mapToInt(session -> valueOrZero(session.getActualMinutes()))
                .sum();
    }

    private Double averageFocus(List<FocusSession> sessions) {
        return roundAverage(findFeedbackBySessionId(sessions).values().stream()
                .map(feedback -> (double) focusScore(feedback.getFocusLevel()))
                .toList());
    }

    private Map<Long, SessionFeedback> findFeedbackBySessionId(List<FocusSession> sessions) {
        List<Long> sessionIds = sessions.stream()
                .map(FocusSession::getSessionId)
                .toList();
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        return sessionFeedbackRepository.findAllBySessionSessionIdIn(sessionIds)
                .stream()
                .collect(Collectors.toMap(feedback -> feedback.getSession().getSessionId(), Function.identity()));
    }

    private TypeStatsResponse.TypeStatsItem toTypeStatsItem(UserTaskTypeProfile profile) {
        return new TypeStatsResponse.TypeStatsItem(
                profile.getTaskType().getTaskTypeId(),
                profile.getTaskType().getCode(),
                profile.getTaskType().getNameKo(),
                profile.getSampleCount(),
                profile.getSumPlannedMinutes(),
                profile.getSumActualMinutes(),
                profile.getErrorRatio(),
                profile.getBiasCorrectionFactor(),
                profile.getLastCalculatedAt()
        );
    }

    private DifficultyCorrectionResponse.DifficultyCorrectionItem toDifficultyCorrectionItem(
            Task.Difficulty difficulty,
            UserAiDifficultyResidual residual
    ) {
        BigDecimal residualValue = residual != null ? residual.getResidual() : BigDecimal.ZERO;
        return new DifficultyCorrectionResponse.DifficultyCorrectionItem(
                difficulty.name(),
                difficultyLabel(difficulty),
                residual != null ? residual.getSampleCount() : 0,
                residualValue,
                residualValue.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                residual != null ? residual.getUpdatedAt() : null
        );
    }

    private String difficultyLabel(Task.Difficulty difficulty) {
        return switch (difficulty) {
            case LOW -> "쉬움";
            case MEDIUM -> "보통";
            case HIGH -> "어려움";
            case UNKNOWN -> "알 수 없음";
        };
    }

    private int focusScore(SessionFeedback.FocusLevel focusLevel) {
        return switch (focusLevel) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case VERY_HIGH -> 4;
        };
    }

    private Double roundAverage(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        double average = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(average)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double diff(Double current, Double previous) {
        if (current == null || previous == null) {
            return null;
        }
        return BigDecimal.valueOf(current - previous)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
