package capstone2.team3.realplan.domain.dailyplan.service;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
     * TODO: Python /tasks/recommend 연동
     */
    public RecommendResponse getRecommendations(Long userId, Long planId) {
        DailyPlan plan = getPlanOrThrow(userId, planId);
        List<Task> candidates = taskRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(t -> t.getStatus() != Task.Status.COMPLETED)
                .filter(t -> t.getRemainingMin() == null || t.getRemainingMin() > 0)
                .sorted((a, b) -> {
                    if (a.getDueDate() == null) return 1;
                    if (b.getDueDate() == null) return -1;
                    return a.getDueDate().compareTo(b.getDueDate());
                })
                .limit(4).toList();

        List<RecommendResponse.RecommendItem> items = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Task t = candidates.get(i);
            items.add(new RecommendResponse.RecommendItem(
                    i + 1, t.getTaskId(), t.getName(),
                    t.getRemainingMin() != null ? t.getRemainingMin() : 0,
                    0.0, 0, 0, 0, false,
                    t.getDueDate() != null ? "D-?" : "마감 없음",
                    "중요도 " + t.getImportance().name(),
                    "12-18", "12-18시", "FLEXIBLE", "마감일 기준 추천되었습니다."));
        }
        return new RecommendResponse(plan.getPlanDate().toString(), plan.getAvailableMinutes(),
                items, items.isEmpty() ? "추천할 미완료 태스크가 없어요." : null);
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
     * 3단계 - B: 슬롯 일괄 배정 (AI 자동배치 결과 적용)
     * AI /schedules/auto-place 결과의 slotIndexes 또는 startTime/endTime을 받아 일괄 배정
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

        // 기존 슬롯 배정 초기화
        dailyPlanSlotRepository.findAllByDailyPlanDailyPlanIdOrderBySlotIndexAsc(planId)
                .forEach(DailyPlanSlot::unassignTask);

        // 기존 AI 배정 플랜 태스크 제거
        dailyPlanTaskRepository.findAllByDailyPlanDailyPlanIdOrderByDisplayOrderAsc(planId)
                .stream()
                .filter(dpt -> dpt.getSourceType() == DailyPlanTask.SourceType.AI)
                .forEach(dailyPlanTaskRepository::delete);
        dailyPlanTaskRepository.flush();

        Set<Integer> assignedSlotIndexes = new HashSet<>();

        // scheduleBlocks 순회하며 슬롯에 태스크 배정
        for (SlotBatchAssignRequest.ScheduleBlock block : request.getScheduleBlocks()) {
            DailyPlanTask planTask = getOrCreatePlanTask(plan, user, block.getTaskId(),
                    DailyPlanTask.SourceType.AI);

            for (Integer slotIndex : resolveSlotIndexes(block)) {
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
        Task task = taskRepository.findByTaskIdAndUserUserId(taskId, user.getUserId())
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

    private List<Integer> resolveSlotIndexes(SlotBatchAssignRequest.ScheduleBlock block) {
        if (block.getSlotIndexes() != null && !block.getSlotIndexes().isEmpty()) {
            return validateSlots(block.getSlotIndexes());
        }
        if (block.getStartTime() == null || block.getEndTime() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        int startSlot = timeToSlotIndex(block.getStartTime());
        int endSlot = timeToSlotIndex(block.getEndTime());
        if (startSlot >= endSlot) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        List<Integer> indexes = new ArrayList<>();
        for (int index = startSlot; index < endSlot; index++) {
            indexes.add(index);
        }
        return validateSlots(indexes);
    }

    private int timeToSlotIndex(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if ((minute != 0 && minute != 30) || hour < 6 || hour > 27) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            int minutesFromStart = (hour * 60 + minute) - (6 * 60);
            if (minutesFromStart < 0 || minutesFromStart > 21 * 60) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return minutesFromStart / 30;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
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
