package capstone2.team3.realplan.domain.dailyplan.service;

import capstone2.team3.realplan.domain.ai.client.AiClient;
import capstone2.team3.realplan.domain.ai.dto.AiScheduleAutoPlaceRequest;
import capstone2.team3.realplan.domain.ai.dto.AiScheduleAutoPlaceResponse;
import capstone2.team3.realplan.domain.ai.dto.AiTaskDecomposeRequest;
import capstone2.team3.realplan.domain.ai.dto.AiTaskDecomposeResponse;
import capstone2.team3.realplan.domain.ai.dto.AiTaskRecommendRequest;
import capstone2.team3.realplan.domain.ai.dto.AiTaskRecommendResponse;
import capstone2.team3.realplan.domain.dailyplan.dto.*;
import capstone2.team3.realplan.domain.dailyplan.entity.*;
import capstone2.team3.realplan.domain.dailyplan.repository.*;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.task.repository.TaskRepository;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPlanService {

    private final DailyPlanRepository dailyPlanRepository;
    private final DailyPlanSlotRepository dailyPlanSlotRepository;
    private final DailyPlanTaskRepository dailyPlanTaskRepository;
    private final DailyPlanSessionRepository dailyPlanSessionRepository;
    private final DailyPlanSessionBlockRepository dailyPlanSessionBlockRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AiClient aiClient;

    // ── 플랜 조회 ─────────────────────────────────────────────────

    public DailyPlanResponse getPlan(Long userId, LocalDate date) {
        DailyPlan plan = dailyPlanRepository.findByUserUserIdAndPlanDate(userId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
        return buildResponse(plan);
    }

    // ── 플랜 생성 ─────────────────────────────────────────────────

    /**
     * 1단계: 플랜 생성
     * 가용시간 슬롯만 저장. 태스크 배정은 이후 단계에서 진행
     */
    @Transactional
    public DailyPlanResponse createPlan(Long userId, DailyPlanCreateRequest request) {
        if (dailyPlanRepository.existsByUserUserIdAndPlanDate(userId, request.getPlanDate())) {
            throw new BusinessException(ErrorCode.DUPLICATE_DAILY_PLAN);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Integer> slotIndexes = validateSlots(request.getSlotIndexes());
        int availableMinutes = slotIndexes.size() * 30;

        DailyPlan plan = DailyPlan.builder()
                .user(user).planDate(request.getPlanDate())
                .availableMinutes(availableMinutes).totalMinutes(0)
                .build();
        dailyPlanRepository.save(plan);
        saveSlots(plan, user, slotIndexes);
        return buildResponse(plan);
    }

    // ── AI 태스크 추천 ────────────────────────────────────────────

    /**
     * 2단계: AI 태스크 추천
     * DB 저장 없이 추천 결과만 반환
     */
    public RecommendResponse getRecommendations(Long userId, Long planId) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        List<Task> candidates = taskRepository.findAllByUserUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .filter(t -> t.getStatus() != Task.Status.COMPLETED)
                .filter(t -> t.getRemainingMin() == null || t.getRemainingMin() > 0)
                .toList();

        AiTaskRecommendResponse aiResponse = aiClient.recommendTasks(new AiTaskRecommendRequest(
                plan.getPlanDate().toString(),
                LocalDateTime.now().toString(),
                plan.getAvailableMinutes(),
                defaultTimeBandFocusScores(),
                candidates.stream().map(this::toRecommendTaskItem).toList()
        ));

        return toRecommendResponse(aiResponse);
    }

    // ── 슬롯 태스크 배정 ──────────────────────────────────────────

    /**
     * 3단계 - A: 슬롯 단건 배정 (사용자 직접)
     * 슬롯 하나에 태스크를 직접 배정하거나 해제
     *
     * 배정 시:
     * - 해당 태스크가 DailyPlanTask에 없으면 자동 생성 (sourceType=USER)
     * - DailyPlanSlot.dailyPlanTask 업데이트
     * - DailyPlan.totalMinutes 재계산
     *
     * 해제 시 (taskId=null):
     * - DailyPlanSlot.dailyPlanTask = null
     * - DailyPlan.totalMinutes 재계산
     */
    @Transactional
    public DailyPlanResponse assignTaskToSlot(Long userId, Long planId, Long slotId,
                                              SlotAssignRequest request) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DailyPlanSlot slot = dailyPlanSlotRepository
                .findBySlotIdAndDailyPlanDailyPlanId(slotId, planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

        if (request.getTaskId() == null) {
            slot.unassignTask();
        } else {
            // 플랜에 태스크가 없으면 자동 생성
            DailyPlanTask planTask = getOrCreatePlanTask(plan, user, request.getTaskId(),
                    DailyPlanTask.SourceType.USER);
            slot.assignTask(planTask);
        }

        recalculateTotalMinutes(planId, plan);
        return buildResponse(plan);
    }

    /**
     * 3단계 - A: 여러 슬롯 직접 배정
     */
    @Transactional
    public DailyPlanResponse assignTaskToSlots(Long userId, Long planId, DailyPlanTaskAssignRequest request) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<Integer> slotIndexes = validateSlots(request.getSlotIndexes());
        DailyPlanTask planTask = getOrCreatePlanTask(plan, user, request.getTaskId(), DailyPlanTask.SourceType.USER);

        for (Integer slotIndex : slotIndexes) {
            DailyPlanSlot slot = dailyPlanSlotRepository
                    .findByDailyPlanDailyPlanIdAndSlotIndex(planId, slotIndex)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
            slot.assignTask(planTask);
        }

        recalculateTotalMinutes(planId, plan);
        return buildResponse(plan);
    }

    /**
     * 3단계 - B: AI 연동 전 fallback 자동 배치
     * 추후 이 메서드 안에서 Python /tasks/decompose, /schedules/auto-place 호출로 교체
     */
    @Transactional
    public DailyPlanResponse autoAssignTasks(Long userId, Long planId, DailyPlanAutoAssignRequest request) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<DailyPlanSlot> availableSlots = dailyPlanSlotRepository
                .findAllByDailyPlanDailyPlanIdOrderBySlotIndexAsc(planId);
        if (availableSlots.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        clearAiAssignments(planId);

        List<Task> tasks = resolveAutoAssignTasks(userId, request);
        int maxContinuousMinutes = request.getMaxContinuousSchedulableMinutes() != null
                ? request.getMaxContinuousSchedulableMinutes() : 90;
        if (maxContinuousMinutes % 30 != 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        AiTaskDecomposeResponse decomposeResponse = aiClient.decomposeTasks(new AiTaskDecomposeRequest(
                30,
                maxContinuousMinutes,
                tasks.stream().map(this::toDecomposeTaskItem).toList()
        ));

        Map<Long, DailyPlanTask> planTasksByTaskId = new HashMap<>();
        Map<Long, Integer> sessionOrderByPlanTaskId = new HashMap<>();
        List<DailyPlanSession> sessions = new ArrayList<>();
        for (Task task : tasks) {
            DailyPlanTask planTask = getOrCreatePlanTask(plan, user, task.getTaskId(), DailyPlanTask.SourceType.AI);
            dailyPlanSessionRepository.deleteAllByDailyPlanTaskDailyPlanTaskId(planTask.getDailyPlanTaskId());
            planTasksByTaskId.put(task.getTaskId(), planTask);
            sessionOrderByPlanTaskId.put(planTask.getDailyPlanTaskId(), 1);
        }
        dailyPlanSessionRepository.flush();

        for (AiTaskDecomposeResponse.TaskSession aiSession : decomposeResponse.taskSessions()) {
            Task task = tasks.stream()
                    .filter(candidate -> candidate.getTaskId().equals(aiSession.taskId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
            DailyPlanTask planTask = planTasksByTaskId.get(task.getTaskId());
            int sessionOrder = sessionOrderByPlanTaskId.compute(
                    planTask.getDailyPlanTaskId(),
                    (ignored, value) -> value == null ? 2 : value + 1) - 1;

            sessions.add(dailyPlanSessionRepository.save(DailyPlanSession.builder()
                    .dailyPlanTask(planTask)
                    .task(task)
                    .user(user)
                    .sessionOrder(sessionOrder)
                    .sessionMinutes(aiSession.sessionMinutes())
                    .requiredFocusLevel(parseRequiredFocusLevel(aiSession.requiredFocusLevel()))
                    .sourceType(DailyPlanSession.SourceType.AI)
                    .status(DailyPlanSession.SessionPlanStatus.PLANNED)
                    .build()));
        }

        AiScheduleAutoPlaceResponse autoPlaceResponse = aiClient.autoPlaceSchedule(new AiScheduleAutoPlaceRequest(
                30,
                maxContinuousMinutes,
                toSchedulableTimeBlocks(availableSlots),
                List.of(),
                tasks.stream().map(task -> toAutoPlaceTaskItem(task, plan.getPlanDate())).toList(),
                sessions.stream().map(this::toAutoPlaceTaskSession).toList()
        ));

        Set<Integer> assignedSlotIndexes = new HashSet<>();
        Map<Long, Integer> maxSlotCountByTaskId = new HashMap<>();
        Map<Long, Integer> assignedSlotCountByTaskId = new HashMap<>();
        for (Task task : tasks) {
            maxSlotCountByTaskId.put(task.getTaskId(), resolveTargetSlotCount(task));
        }
        Map<Long, Integer> nextBlockOrderBySessionId = new HashMap<>();
        for (AiScheduleAutoPlaceResponse.ScheduleBlock block : autoPlaceResponse.scheduleBlocks()) {
            DailyPlanSession session = sessions.stream()
                    .filter(candidate -> candidate.getDailyPlanSessionId().equals(block.dailyPlanSessionId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
            DailyPlanTask planTask = session.getDailyPlanTask();
            Long taskId = planTask.getTask().getTaskId();
            int maxSlotCount = maxSlotCountByTaskId.getOrDefault(taskId, Integer.MAX_VALUE);
            int assignedSlotCount = assignedSlotCountByTaskId.getOrDefault(taskId, 0);

            List<DailyPlanSlot> assignedSlots = new ArrayList<>();
            for (Integer slotIndex : validateSlots(block.slotIndexes())) {
                if (assignedSlotCount >= maxSlotCount) {
                    log.warn("AI schedule block exceeds task target minutes. taskId={}, maxSlots={}, ignoredSlotIndex={}",
                            taskId, maxSlotCount, slotIndex);
                    continue;
                }
                if (!assignedSlotIndexes.add(slotIndex)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                }
                DailyPlanSlot slot = dailyPlanSlotRepository
                        .findByDailyPlanDailyPlanIdAndSlotIndex(planId, slotIndex)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
                slot.assignTask(planTask);
                assignedSlots.add(slot);
                assignedSlotCount++;
            }
            if (assignedSlots.isEmpty()) {
                continue;
            }
            assignedSlotCountByTaskId.put(taskId, assignedSlotCount);
            session.markScheduled();
            int nextBlockOrder = nextBlockOrderBySessionId.getOrDefault(session.getDailyPlanSessionId(), 1);
            int savedBlockCount = saveSessionBlocks(session, assignedSlots, nextBlockOrder);
            nextBlockOrderBySessionId.put(session.getDailyPlanSessionId(), nextBlockOrder + savedBlockCount);
        }

        for (AiScheduleAutoPlaceResponse.UnscheduledSession unscheduled : autoPlaceResponse.unscheduledSessions()) {
            sessions.stream()
                    .filter(session -> session.getDailyPlanSessionId().equals(unscheduled.dailyPlanSessionId()))
                    .findFirst()
                    .ifPresent(session -> session.markUnscheduled(unscheduled.reasonCode()));
        }

        recalculateTotalMinutes(planId, plan);
        return buildResponse(plan);
    }

    /**
     * 3단계 - B: 슬롯 일괄 배정 (AI 자동배치 결과 적용)
     * AI /schedules/auto-place 결과의 slotIndexes를 받아 일괄 배정
     *
     * 동작:
     * 1. 기존 슬롯 배정 전체 초기화
     * 2. scheduleBlocks 순회
     *    - 태스크가 DailyPlanTask에 없으면 자동 생성 (sourceType=AI)
     *    - slotIndexes의 각 슬롯에 해당 태스크 배정
     * 3. totalMinutes 재계산
     */
    @Transactional
    public DailyPlanResponse batchAssignTasks(Long userId, Long planId,
                                              SlotBatchAssignRequest request) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        clearAiAssignments(planId);

        Set<Integer> assignedSlotIndexes = new HashSet<>();

        // scheduleBlocks 순회하며 슬롯에 태스크 배정
        for (SlotBatchAssignRequest.ScheduleBlock block : request.getScheduleBlocks()) {
            DailyPlanTask planTask = getOrCreatePlanTask(plan, user, block.getTaskId(),
                    DailyPlanTask.SourceType.AI);

            for (Integer slotIndex : validateSlots(block.getSlotIndexes())) {
                if (!assignedSlotIndexes.add(slotIndex)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                }

                DailyPlanSlot slot = dailyPlanSlotRepository
                        .findByDailyPlanDailyPlanIdAndSlotIndex(planId, slotIndex)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
                slot.assignTask(planTask);
            }
        }

        recalculateTotalMinutes(planId, plan);
        return buildResponse(plan);
    }

    // ── 플랜 상태 변경 ────────────────────────────────────────────

    @Transactional
    public DailyPlanResponse updatePlanStatus(Long userId, Long planId, DailyPlanStatusRequest request) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        switch (request.getStatus()) {
            case "CONFIRMED" -> plan.confirm();
            case "REJECTED"  -> plan.reject();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return buildResponse(plan);
    }

    // ── 슬롯 교체 ─────────────────────────────────────────────────

    @Transactional
    public DailyPlanResponse updateSlots(Long userId, Long planId, SlotUpdateRequest request) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        dailyPlanSlotRepository.deleteAllByDailyPlanDailyPlanId(planId);
        dailyPlanSlotRepository.flush();

        List<Integer> slotIndexes = validateSlots(request.getSlotIndexes());
        saveSlots(plan, user, slotIndexes);
        plan.updateAvailableMinutes(slotIndexes.size() * 30);
        recalculateTotalMinutes(planId, plan);
        return buildResponse(plan);
    }

    // ── 플랜 태스크 수정 ──────────────────────────────────────────

    @Transactional
    public DailyPlanResponse updatePlanTask(Long userId, Long planId, Long dailyPlanTaskId,
                                            DailyPlanTaskUpdateRequest request) {
        getPlanOrThrow(userId, planId);
        DailyPlanTask planTask = dailyPlanTaskRepository
                .findByDailyPlanTaskIdAndDailyPlanDailyPlanId(dailyPlanTaskId, planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        if (request.getIsSelected() != null) {
            if (request.getIsSelected()) planTask.select();
            else planTask.deselect();
        }
        if (request.getDisplayOrder() != null) planTask.updateDisplayOrder(request.getDisplayOrder());
        return buildResponse(getPlanOrThrow(userId, planId));
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private DailyPlan getPlanOrThrow(Long userId, Long planId) {
        return dailyPlanRepository.findByDailyPlanIdAndUserUserId(planId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    }

    /**
     * 플랜에 태스크가 없으면 자동 생성, 있으면 기존 것 반환
     * plannedMinutes는 슬롯 배정 후 recalculate로 자동 계산
     */
    private DailyPlanTask getOrCreatePlanTask(DailyPlan plan, User user, Long taskId,
                                              DailyPlanTask.SourceType sourceType) {
        Task task = taskRepository.findByTaskIdAndUserUserIdAndDeletedAtIsNull(taskId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        return dailyPlanTaskRepository
                .findByDailyPlanDailyPlanIdAndTaskTaskId(plan.getDailyPlanId(), taskId)
                .orElseGet(() -> {
                    int nextOrder = dailyPlanTaskRepository
                            .findAllByDailyPlanDailyPlanIdOrderByDisplayOrderAsc(plan.getDailyPlanId())
                            .size() + 1;
                    return dailyPlanTaskRepository.save(DailyPlanTask.builder()
                            .dailyPlan(plan).task(task).displayOrder(nextOrder)
                            .sourceType(sourceType).plannedMinutes(0).isSelected(true)
                            .build());
                });
    }

    private void clearAiAssignments(Long planId) {
        dailyPlanSlotRepository.findAllByDailyPlanDailyPlanIdOrderBySlotIndexAsc(planId)
                .forEach(DailyPlanSlot::unassignTask);

        dailyPlanTaskRepository.findAllByDailyPlanDailyPlanIdOrderByDisplayOrderAsc(planId)
                .stream()
                .filter(dpt -> dpt.getSourceType() == DailyPlanTask.SourceType.AI)
                .forEach(dailyPlanTaskRepository::delete);
        dailyPlanTaskRepository.flush();
    }

    private List<Task> resolveAutoAssignTasks(Long userId, DailyPlanAutoAssignRequest request) {
        if (request.getTaskIds() != null && !request.getTaskIds().isEmpty()) {
            return request.getTaskIds().stream()
                    .distinct()
                    .map(taskId -> taskRepository.findByTaskIdAndUserUserIdAndDeletedAtIsNull(taskId, userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND)))
                    .filter(task -> task.getStatus() != Task.Status.COMPLETED)
                    .toList();
        }

        int maxTasks = request.getMaxTasks() != null ? request.getMaxTasks() : 4;
        return taskRepository.findAllByUserUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .filter(task -> task.getStatus() != Task.Status.COMPLETED)
                .filter(task -> resolveTargetMinutes(task) > 0)
                .sorted(Comparator
                        .comparing((Task task) -> task.getDueDate() == null)
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(task -> switch (task.getImportance()) {
                            case HIGH -> 0;
                            case MEDIUM -> 1;
                            case LOW -> 2;
                        }))
                .limit(maxTasks)
                .toList();
    }

    private int resolveTargetMinutes(Task task) {
        if (task.getRemainingMin() != null && task.getRemainingMin() > 0) {
            return task.getRemainingMin();
        }
        if (task.getFinalEstimated() != null && task.getFinalEstimated() > 0) {
            return task.getFinalEstimated();
        }
        if (task.getUserEstimated() != null && task.getUserEstimated() > 0) {
            return task.getUserEstimated();
        }
        return 30;
    }

    private int resolveTargetSlotCount(Task task) {
        return Math.max(1, (int) Math.ceil(resolveTargetMinutes(task) / 30.0));
    }

    private List<AiTaskRecommendRequest.TimeBandFocusScore> defaultTimeBandFocusScores() {
        return List.of(
                new AiTaskRecommendRequest.TimeBandFocusScore("06-12", 85),
                new AiTaskRecommendRequest.TimeBandFocusScore("12-18", 65),
                new AiTaskRecommendRequest.TimeBandFocusScore("18-24", 45)
        );
    }

    private AiTaskRecommendRequest.TaskItem toRecommendTaskItem(Task task) {
        return new AiTaskRecommendRequest.TaskItem(
                task.getTaskId(),
                task.getName(),
                task.getDueDate() != null ? task.getDueDate().toString() : null,
                task.getImportance().name(),
                task.getTaskType().getCode(),
                task.getDifficulty().name(),
                task.getStatus().name(),
                resolveTargetMinutes(task),
                0
        );
    }

    private RecommendResponse toRecommendResponse(AiTaskRecommendResponse response) {
        return new RecommendResponse(
                response.targetDate(),
                response.availableMinutes(),
                response.recommendations().stream()
                        .map(item -> new RecommendResponse.RecommendItem(
                                item.rank(),
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
                        ))
                        .toList(),
                response.message()
        );
    }

    private AiTaskDecomposeRequest.TaskItem toDecomposeTaskItem(Task task) {
        return new AiTaskDecomposeRequest.TaskItem(
                task.getTaskId(),
                task.getName(),
                task.getDescription(),
                task.getTaskType().getCode(),
                task.getDifficulty().name(),
                resolveTargetMinutes(task),
                0
        );
    }

    private DailyPlanSession.RequiredFocusLevel parseRequiredFocusLevel(String value) {
        try {
            return DailyPlanSession.RequiredFocusLevel.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private List<AiScheduleAutoPlaceRequest.SchedulableTimeBlock> toSchedulableTimeBlocks(
            List<DailyPlanSlot> slots
    ) {
        List<Integer> sortedSlotIndexes = slots.stream()
                .map(DailyPlanSlot::getSlotIndex)
                .sorted()
                .toList();
        if (sortedSlotIndexes.isEmpty()) {
            return List.of();
        }

        List<AiScheduleAutoPlaceRequest.SchedulableTimeBlock> blocks = new ArrayList<>();
        int blockStartIndex = sortedSlotIndexes.get(0);
        int previousIndex = blockStartIndex;
        for (int i = 1; i <= sortedSlotIndexes.size(); i++) {
            boolean shouldClose = i == sortedSlotIndexes.size()
                    || sortedSlotIndexes.get(i) != previousIndex + 1;
            if (shouldClose) {
                blocks.add(new AiScheduleAutoPlaceRequest.SchedulableTimeBlock(
                        slotIndexToTime(blockStartIndex),
                        slotIndexToTime(previousIndex + 1)
                ));
                if (i < sortedSlotIndexes.size()) {
                    blockStartIndex = sortedSlotIndexes.get(i);
                }
            }
            if (i < sortedSlotIndexes.size()) {
                previousIndex = sortedSlotIndexes.get(i);
            }
        }
        return blocks;
    }

    private AiScheduleAutoPlaceRequest.TaskItem toAutoPlaceTaskItem(Task task, LocalDate planDate) {
        boolean isDueToday = task.getDueDate() != null
                && task.getDueDate().toLocalDate().equals(planDate);
        return new AiScheduleAutoPlaceRequest.TaskItem(
                task.getTaskId(),
                isDueToday,
                importanceScore(task.getImportance()),
                resolveTargetMinutes(task),
                task.getDifficulty().name()
        );
    }

    private AiScheduleAutoPlaceRequest.TaskSession toAutoPlaceTaskSession(DailyPlanSession session) {
        return new AiScheduleAutoPlaceRequest.TaskSession(
                session.getDailyPlanSessionId(),
                session.getTask().getTaskId(),
                session.getSessionMinutes(),
                session.getRequiredFocusLevel().name()
        );
    }

    private int importanceScore(Task.Importance importance) {
        return switch (importance) {
            case HIGH -> 100;
            case MEDIUM -> 60;
            case LOW -> 30;
        };
    }

    private void saveFallbackDailyPlanSession(DailyPlanTask planTask, Task task, User user,
                                              List<DailyPlanSlot> assignedSlots,
                                              int maxContinuousMinutes) {
        int sessionOrder = 1;
        int maxSlotsPerSession = Math.max(1, maxContinuousMinutes / 30);
        for (int start = 0; start < assignedSlots.size(); start += maxSlotsPerSession) {
            int end = Math.min(start + maxSlotsPerSession, assignedSlots.size());
            List<DailyPlanSlot> sessionSlots = assignedSlots.subList(start, end);

            DailyPlanSession session = dailyPlanSessionRepository.save(DailyPlanSession.builder()
                    .dailyPlanTask(planTask)
                    .task(task)
                    .user(user)
                    .sessionOrder(sessionOrder++)
                    .sessionMinutes(sessionSlots.size() * 30)
                    .requiredFocusLevel(resolveRequiredFocusLevel(task))
                    .sourceType(DailyPlanSession.SourceType.AI)
                    .status(DailyPlanSession.SessionPlanStatus.SCHEDULED)
                    .build());

            saveFallbackSessionBlocks(session, sessionSlots);
        }
    }

    private void saveFallbackSessionBlocks(DailyPlanSession session, List<DailyPlanSlot> slots) {
        saveSessionBlocks(session, slots, 1);
    }

    private int saveSessionBlocks(DailyPlanSession session, List<DailyPlanSlot> slots, int startBlockOrder) {
        List<DailyPlanSlot> sortedSlots = slots.stream()
                .sorted(Comparator.comparingInt(DailyPlanSlot::getSlotIndex))
                .toList();
        if (sortedSlots.isEmpty()) {
            return 0;
        }
        int blockOrder = startBlockOrder;
        int savedBlockCount = 0;
        int blockStartIndex = sortedSlots.get(0).getSlotIndex();
        int previousIndex = blockStartIndex;

        for (int i = 1; i <= sortedSlots.size(); i++) {
            boolean shouldClose = i == sortedSlots.size()
                    || sortedSlots.get(i).getSlotIndex() != previousIndex + 1;
            if (shouldClose) {
                dailyPlanSessionBlockRepository.save(DailyPlanSessionBlock.builder()
                        .dailyPlanSession(session)
                        .blockOrder(blockOrder++)
                        .startTime(slotIndexToTime(blockStartIndex))
                        .endTime(slotIndexToTime(previousIndex + 1))
                        .durationMinutes((previousIndex - blockStartIndex + 1) * 30)
                        .build());
                savedBlockCount++;
                if (i < sortedSlots.size()) {
                    blockStartIndex = sortedSlots.get(i).getSlotIndex();
                }
            }
            if (i < sortedSlots.size()) {
                previousIndex = sortedSlots.get(i).getSlotIndex();
            }
        }
        return savedBlockCount;
    }

    private DailyPlanSession.RequiredFocusLevel resolveRequiredFocusLevel(Task task) {
        return switch (task.getDifficulty()) {
            case HIGH -> DailyPlanSession.RequiredFocusLevel.HIGH;
            case MEDIUM -> task.getImportance() == Task.Importance.HIGH
                    ? DailyPlanSession.RequiredFocusLevel.HIGH
                    : DailyPlanSession.RequiredFocusLevel.MEDIUM;
            case LOW -> DailyPlanSession.RequiredFocusLevel.LOW;
            case UNKNOWN -> task.getImportance() == Task.Importance.HIGH
                    ? DailyPlanSession.RequiredFocusLevel.MEDIUM
                    : DailyPlanSession.RequiredFocusLevel.FLEXIBLE;
        };
    }

    private String slotIndexToTime(int slotIndex) {
        int totalMinutes = 6 * 60 + slotIndex * 30;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * 슬롯 배정 변경 후 DailyPlanTask.plannedMinutes 재계산
     * plannedMinutes = 해당 태스크에 배정된 슬롯 수 × 30
     */
    private void recalculateTotalMinutes(Long planId, DailyPlan plan) {
        List<DailyPlanSlot> slots = dailyPlanSlotRepository
                .findAllByDailyPlanDailyPlanIdOrderBySlotIndexAsc(planId);

        dailyPlanTaskRepository.findAllByDailyPlanDailyPlanIdOrderByDisplayOrderAsc(planId)
                .forEach(planTask -> planTask.updatePlannedMinutes(0));

        // 태스크별 배정 슬롯 수 계산
        slots.stream()
                .filter(s -> s.getDailyPlanTask() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> s.getDailyPlanTask().getDailyPlanTaskId(),
                        java.util.stream.Collectors.counting()))
                .forEach((planTaskId, count) ->
                        dailyPlanTaskRepository.findById(planTaskId)
                                .ifPresent(pt -> pt.updatePlannedMinutes(count.intValue() * 30)));

        // totalMinutes = 배정된 슬롯 수 × 30
        int totalMinutes = (int) slots.stream()
                .filter(s -> s.getDailyPlanTask() != null).count() * 30;
        plan.updateTotalMinutes(totalMinutes);
    }

    private List<Integer> validateSlots(List<Integer> slotIndexes) {
        if (slotIndexes == null) return List.of();
        if (slotIndexes.stream().anyMatch(i -> i == null || i < 0 || i > 41)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<Integer> valid = slotIndexes.stream().distinct().toList();
        if (valid.size() != slotIndexes.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return valid;
    }

    private void saveSlots(DailyPlan plan, User user, List<Integer> slotIndexes) {
        dailyPlanSlotRepository.saveAll(slotIndexes.stream()
                .map(index -> DailyPlanSlot.builder()
                        .dailyPlan(plan).user(user).slotIndex(index).build())
                .toList());
    }

    private DailyPlanResponse buildResponse(DailyPlan plan) {
        return DailyPlanResponse.of(plan,
                dailyPlanSlotRepository.findAllByDailyPlanDailyPlanIdOrderBySlotIndexAsc(plan.getDailyPlanId()),
                dailyPlanTaskRepository.findAllByDailyPlanDailyPlanIdOrderByDisplayOrderAsc(plan.getDailyPlanId()));
    }
}
