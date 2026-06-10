package capstone2.team3.realplan.domain.ai.client;

import capstone2.team3.realplan.domain.ai.dto.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class FallbackAiClient implements AiClient {

    private static final int SLOT_UNIT_MINUTES = 30;

    @Override
    public AiHealthResponse health() {
        return new AiHealthResponse("ok", "realplan-ai-fallback");
    }

    @Override
    public AiTaskClassifyResponse classifyTask(AiTaskClassifyRequest request) {
        return new AiTaskClassifyResponse("SATISFACTION_BASED", "AI 연동 전 fallback 분류입니다.", "fallback");
    }

    @Override
    public AiTaskEstimateResponse estimateTask(AiTaskEstimateRequest request) {
        return new AiTaskEstimateResponse(request.estimatedMinutes(), 1.0, 0.0, "RULE");
    }

    @Override
    public AiSessionEstimateResponse estimateSession(AiSessionEstimateRequest request) {
        double progressBasedRemaining = request.elapsedMinutes() * (1 / request.progress() - 1);
        double focusWeight = focusWeight(request.focusLevel());
        double normalizedRemaining = progressBasedRemaining * focusWeight;
        double blendingWeight = 0.4 * request.progress();
        double updatedTotal = blendingWeight * (request.elapsedMinutes() + normalizedRemaining)
                + (1 - blendingWeight) * request.previousAiTotalMinutes();
        double finalRemaining = Math.max(0, updatedTotal - request.elapsedMinutes());
        if (finalRemaining <= 0 && request.progress() < 1.0) {
            finalRemaining = SLOT_UNIT_MINUTES;
        }

        return new AiSessionEstimateResponse(
                progressBasedRemaining,
                normalizedRemaining,
                blendingWeight,
                finalRemaining,
                updatedTotal,
                focusWeight
        );
    }

    @Override
    public AiPlanningErrorRateResponse updatePlanningErrorRates(AiPlanningErrorRateRequest request) {
        double ratio = (double) request.actualMinutes() / request.estimatedMinutes();
        double logRatio = Math.log(ratio);
        Map<String, Double> userTypeResidual = copyDoubleMap(request.userTypeResidual());
        Map<String, Double> userDifficultyResidual = copyDoubleMap(request.userDifficultyResidual());
        Map<String, Double> userFolderResidual = copyDoubleMap(request.userFolderResidual());
        Map<String, Integer> typeCount = copyIntegerMap(request.typeCount());
        Map<String, Integer> difficultyCount = copyIntegerMap(request.difficultyCount());
        Map<String, Integer> folderCount = copyIntegerMap(request.folderCount());

        userTypeResidual.put(request.taskType(), logRatio);
        userDifficultyResidual.put(request.difficulty(), logRatio);
        typeCount.put(request.taskType(), typeCount.getOrDefault(request.taskType(), 0) + 1);
        difficultyCount.put(request.difficulty(), difficultyCount.getOrDefault(request.difficulty(), 0) + 1);
        if (request.folderId() != null) {
            userFolderResidual.put(request.folderId(), logRatio);
            folderCount.put(request.folderId(), folderCount.getOrDefault(request.folderId(), 0) + 1);
        }

        return new AiPlanningErrorRateResponse(
                logRatio,
                userTypeResidual,
                userDifficultyResidual,
                userFolderResidual,
                typeCount,
                difficultyCount,
                folderCount,
                ratio,
                ratio,
                logRatio,
                logRatio,
                "AVERAGE_BASELINE",
                false,
                null
        );
    }

    @Override
    public AiTaskRecommendResponse recommendTasks(AiTaskRecommendRequest request) {
        List<AiTaskRecommendResponse.RecommendationItem> recommendations = request.tasks().stream()
                .filter(task -> task.remainingMin() - task.activeScheduledMin() > 0)
                .sorted(Comparator
                        .comparingInt((AiTaskRecommendRequest.TaskItem task) -> importanceScore(task.importance())).reversed()
                        .thenComparing(AiTaskRecommendRequest.TaskItem::taskId))
                .limit(4)
                .map(task -> toRecommendation(task, request))
                .toList();

        List<AiTaskRecommendResponse.RecommendationItem> ranked = new ArrayList<>();
        for (int i = 0; i < recommendations.size(); i++) {
            AiTaskRecommendResponse.RecommendationItem item = recommendations.get(i);
            ranked.add(new AiTaskRecommendResponse.RecommendationItem(
                    i + 1,
                    item.taskId(),
                    item.name(),
                    item.remainingMin(),
                    item.recommendScore(),
                    item.deadlineScore(),
                    item.workloadUrgencyScore(),
                    item.importanceScore(),
                    item.isDueToday(),
                    item.deadlineLabel(),
                    item.importanceLabel(),
                    item.recommendedTimeBand(),
                    item.recommendedTimeBandLabel(),
                    item.requiredFocusLevel(),
                    item.reason()
            ));
        }

        return new AiTaskRecommendResponse(
                request.targetDate(),
                request.availableMinutes(),
                ranked,
                ranked.isEmpty() ? "추천할 미완료 태스크가 없어요." : null
        );
    }

    @Override
    public AiTaskDecomposeResponse decomposeTasks(AiTaskDecomposeRequest request) {
        List<AiTaskDecomposeResponse.TaskSession> sessions = new ArrayList<>();
        for (AiTaskDecomposeRequest.TaskItem task : request.tasks()) {
            int remaining = Math.max(0, task.remainingMin() - task.activeScheduledMin());
            int maxSession = Math.max(request.slotUnitMinutes(), request.maxContinuousSchedulableMinutes());
            while (remaining > 0) {
                int sessionMinutes = Math.min(remaining, maxSession);
                sessions.add(new AiTaskDecomposeResponse.TaskSession(
                        null,
                        task.taskId(),
                        sessionMinutes,
                        requiredFocusLevel(task.difficulty(), "MEDIUM")
                ));
                remaining -= sessionMinutes;
            }
        }
        return new AiTaskDecomposeResponse(sessions);
    }

    @Override
    public AiScheduleAutoPlaceResponse autoPlaceSchedule(AiScheduleAutoPlaceRequest request) {
        List<Integer> freeSlots = request.schedulableTimeBlocks().stream()
                .flatMap(block -> toSlotIndexes(block.start(), block.end()).stream())
                .distinct()
                .sorted()
                .toList();

        int cursor = 0;
        int scheduledMinutes = 0;
        int unscheduledMinutes = 0;
        List<AiScheduleAutoPlaceResponse.ScheduleBlock> blocks = new ArrayList<>();
        List<AiScheduleAutoPlaceResponse.UnscheduledSession> unscheduled = new ArrayList<>();

        for (AiScheduleAutoPlaceRequest.TaskSession session : request.taskSessions()) {
            int requiredSlots = (int) Math.ceil((double) session.sessionMinutes() / SLOT_UNIT_MINUTES);
            List<Integer> assigned = new ArrayList<>();
            while (cursor < freeSlots.size() && assigned.size() < requiredSlots) {
                assigned.add(freeSlots.get(cursor++));
            }

            if (!assigned.isEmpty()) {
                scheduledMinutes += assigned.size() * SLOT_UNIT_MINUTES;
                blocks.add(new AiScheduleAutoPlaceResponse.ScheduleBlock(
                        session.dailyPlanSessionId(),
                        session.taskId(),
                        assigned
                ));
            }

            int missingSlots = requiredSlots - assigned.size();
            if (missingSlots > 0) {
                int missingMinutes = missingSlots * SLOT_UNIT_MINUTES;
                unscheduledMinutes += missingMinutes;
                unscheduled.add(new AiScheduleAutoPlaceResponse.UnscheduledSession(
                        session.dailyPlanSessionId(),
                        session.taskId(),
                        missingMinutes,
                        "INSUFFICIENT_TIME"
                ));
            }
        }

        return new AiScheduleAutoPlaceResponse(
                blocks,
                unscheduled,
                new AiScheduleAutoPlaceResponse.Summary(
                        scheduledMinutes,
                        unscheduledMinutes,
                        freeSlots.size() * SLOT_UNIT_MINUTES,
                        SLOT_UNIT_MINUTES
                )
        );
    }

    private AiTaskRecommendResponse.RecommendationItem toRecommendation(
            AiTaskRecommendRequest.TaskItem task,
            AiTaskRecommendRequest request
    ) {
        int importanceScore = importanceScore(task.importance());
        int workloadScore = Math.min(100, Math.max(15, task.remainingMin() / 2));
        double recommendScore = 0.7 * workloadScore + 0.3 * importanceScore;
        String requiredFocusLevel = requiredFocusLevel(task.difficulty(), task.importance());
        String recommendedTimeBand = recommendedTimeBand(requiredFocusLevel, task.taskId(), request);

        return new AiTaskRecommendResponse.RecommendationItem(
                0,
                task.taskId(),
                task.name(),
                task.remainingMin(),
                recommendScore,
                10,
                workloadScore,
                importanceScore,
                false,
                task.dueDate() != null ? "D-?" : "마감 없음",
                "중요도 " + task.importance(),
                recommendedTimeBand,
                timeBandLabel(recommendedTimeBand),
                requiredFocusLevel,
                "AI 연동 전 fallback 추천입니다."
        );
    }

    private int importanceScore(String importance) {
        return switch (importance == null ? "MEDIUM" : importance) {
            case "HIGH" -> 100;
            case "LOW" -> 30;
            default -> 60;
        };
    }

    private String requiredFocusLevel(String difficulty, String importance) {
        return switch (difficulty == null ? "UNKNOWN" : difficulty) {
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "HIGH".equals(importance) ? "HIGH" : "MEDIUM";
            case "LOW" -> "LOW";
            default -> "HIGH".equals(importance) ? "MEDIUM" : "FLEXIBLE";
        };
    }

    private String recommendedTimeBand(
            String requiredFocusLevel,
            Long taskId,
            AiTaskRecommendRequest request
    ) {
        List<AiTaskRecommendRequest.TimeBandFocusScore> scores = request.timeBandFocusScores();
        if (scores == null || scores.isEmpty()) {
            return fallbackTimeBand(requiredFocusLevel, taskId);
        }

        return switch (requiredFocusLevel) {
            case "HIGH" -> scores.stream()
                    .max(Comparator
                            .comparingInt(AiTaskRecommendRequest.TimeBandFocusScore::focusScore)
                            .thenComparing(AiTaskRecommendRequest.TimeBandFocusScore::timeBand))
                    .map(AiTaskRecommendRequest.TimeBandFocusScore::timeBand)
                    .orElseGet(() -> fallbackTimeBand(requiredFocusLevel, taskId));
            case "LOW", "FLEXIBLE" -> scores.stream()
                    .min(Comparator
                            .comparingInt(AiTaskRecommendRequest.TimeBandFocusScore::focusScore)
                            .thenComparing(AiTaskRecommendRequest.TimeBandFocusScore::timeBand))
                    .map(AiTaskRecommendRequest.TimeBandFocusScore::timeBand)
                    .orElseGet(() -> fallbackTimeBand(requiredFocusLevel, taskId));
            default -> middleFocusBand(scores, taskId);
        };
    }

    private String middleFocusBand(
            List<AiTaskRecommendRequest.TimeBandFocusScore> scores,
            Long taskId
    ) {
        List<AiTaskRecommendRequest.TimeBandFocusScore> sorted = scores.stream()
                .sorted(Comparator
                        .comparingInt(AiTaskRecommendRequest.TimeBandFocusScore::focusScore)
                        .thenComparing(AiTaskRecommendRequest.TimeBandFocusScore::timeBand))
                .toList();
        if (sorted.isEmpty()) {
            return fallbackTimeBand("MEDIUM", taskId);
        }
        return sorted.get(sorted.size() / 2).timeBand();
    }

    private String fallbackTimeBand(String requiredFocusLevel, Long taskId) {
        return switch (requiredFocusLevel) {
            case "HIGH" -> "06-12";
            case "LOW", "FLEXIBLE" -> "18-24";
            default -> Math.floorMod(taskId == null ? 0 : taskId, 2) == 0 ? "12-18" : "18-24";
        };
    }

    private String timeBandLabel(String timeBand) {
        return switch (timeBand) {
            case "06-12" -> "06-12시";
            case "18-24" -> "18-24시";
            default -> "12-18시";
        };
    }

    private double focusWeight(String focusLevel) {
        return switch (focusLevel == null ? "MEDIUM" : focusLevel) {
            case "LOW" -> 0.8;
            case "HIGH" -> 1.2;
            case "VERY_HIGH" -> 1.5;
            default -> 1.0;
        };
    }

    private List<Integer> toSlotIndexes(String start, String end) {
        int startIndex = timeToSlotIndex(start);
        int endIndex = timeToSlotIndex(end);
        List<Integer> result = new ArrayList<>();
        for (int index = startIndex; index < endIndex; index++) {
            result.add(index);
        }
        return result;
    }

    private int timeToSlotIndex(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return ((hour * 60 + minute) - 6 * 60) / SLOT_UNIT_MINUTES;
    }

    private Map<String, Double> copyDoubleMap(Map<String, Double> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }

    private Map<String, Integer> copyIntegerMap(Map<String, Integer> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }
}
