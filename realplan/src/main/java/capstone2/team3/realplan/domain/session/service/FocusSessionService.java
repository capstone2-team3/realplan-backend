package capstone2.team3.realplan.domain.session.service;

import capstone2.team3.realplan.domain.ai.client.AiClient;
import capstone2.team3.realplan.domain.ai.dto.AiSessionEstimateRequest;
import capstone2.team3.realplan.domain.ai.dto.AiSessionEstimateResponse;
import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanTask;
import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanSession;
import capstone2.team3.realplan.domain.dailyplan.repository.DailyPlanSessionRepository;
import capstone2.team3.realplan.domain.dailyplan.repository.DailyPlanTaskRepository;
import capstone2.team3.realplan.domain.session.dto.*;
import capstone2.team3.realplan.domain.session.entity.*;
import capstone2.team3.realplan.domain.session.repository.*;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import capstone2.team3.realplan.domain.task.repository.TaskRepository;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusSessionService {

    private final FocusSessionRepository focusSessionRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final SessionPauseEventRepository sessionPauseEventRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final DailyPlanTaskRepository dailyPlanTaskRepository;
    private final DailyPlanSessionRepository dailyPlanSessionRepository;
    private final UserTaskTypeProfileRepository userTaskTypeProfileRepository;
    private final AiClient aiClient;

    // ── 세션 목록 조회 ────────────────────────────────

    /**
     * 태스크별 세션 목록 조회
     * 태스크 상세 화면에서 학습 기록 목록 표시용
     */
    public List<SessionResponse> getSessions(Long userId, Long taskId) {
        taskRepository.findByTaskIdAndUserUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        return focusSessionRepository.findAllByTaskTaskIdOrderByStartedAtDesc(taskId)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    // ── 세션 시작 ─────────────────────────────────────

    /**
     * 세션 시작
     * - 이미 진행 중인 세션이 있으면 시작 불가
     * - dailyPlanTaskId가 있으면 플랜에서 시작 (plannedMinutes 저장)
     * - 없으면 즉석 시작 (plannedMinutes = null)
     * - 시작 시점의 태스크 remainingMin을 aiRemainingBefore로 저장
     *   → AI /sessions/estimate 연동 시 기준값으로 사용
     */
    @Transactional
    public SessionResponse startSession(Long userId, SessionStartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Task task = taskRepository.findByTaskIdAndUserUserId(request.getTaskId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        // 사용자는 한 번에 하나의 태스크만 수행할 수 있으므로 기존 진행 세션은 자동 종료
        autoEndRunningSessions(userId);

        // 플랜 태스크 연결 (선택)
        DailyPlanTask dailyPlanTask = null;
        DailyPlanSession dailyPlanSession = null;
        Integer plannedMinutes = null;

        if (request.getDailyPlanSessionId() != null) {
            dailyPlanSession = dailyPlanSessionRepository
                    .findByDailyPlanSessionIdAndUserUserIdAndTaskTaskId(
                            request.getDailyPlanSessionId(), userId, task.getTaskId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
            dailyPlanTask = dailyPlanSession.getDailyPlanTask();
            plannedMinutes = dailyPlanSession.getSessionMinutes();
            dailyPlanSession.markInProgress();
        } else if (request.getDailyPlanTaskId() != null) {
            dailyPlanTask = dailyPlanTaskRepository
                    .findByDailyPlanTaskIdAndDailyPlanUserUserIdAndTaskTaskId(
                            request.getDailyPlanTaskId(), userId, task.getTaskId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
            plannedMinutes = dailyPlanTask.getPlannedMinutes();
        }

        FocusSession session = FocusSession.builder()
                .task(task)
                .user(user)
                .dailyPlanTask(dailyPlanTask)
                .dailyPlanSession(dailyPlanSession)
                .source(FocusSession.SessionSource.SESSION)
                .sessionStatus(FocusSession.SessionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .plannedMinutes(plannedMinutes)
                .aiRemainingBefore(task.getRemainingMin()) // AI 잔여시간 기준값 저장
                .build();

        // 태스크 상태 IN_PROGRESS로 변경
        if (task.getStatus() == Task.Status.PENDING) {
            task.updateProgress(task.getProgressPercent(), 0);
        }

        return buildResponse(focusSessionRepository.save(session));
    }

    // ── 일시정지 ──────────────────────────────────────

    /**
     * 세션 일시정지
     * SessionPauseEvent 생성 (pausedAt 저장, resumedAt=null)
     */
    @Transactional
    public SessionResponse pauseSession(Long userId, Long sessionId) {
        FocusSession session = getActiveSessionOrThrow(userId, sessionId);

        if (session.getSessionStatus() != FocusSession.SessionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        session.pause();
        sessionPauseEventRepository.save(SessionPauseEvent.builder()
                .session(session)
                .pausedAt(LocalDateTime.now())
                .build());

        return buildResponse(session);
    }

    // ── 재개 ──────────────────────────────────────────

    /**
     * 세션 재개
     * 가장 최근 미재개 PauseEvent의 resumedAt 업데이트
     */
    @Transactional
    public SessionResponse resumeSession(Long userId, Long sessionId) {
        FocusSession session = getSessionOrThrow(userId, sessionId);

        if (session.getSessionStatus() != FocusSession.SessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        session.resume();

        // 가장 최근 일시정지 이벤트 재개 처리
        sessionPauseEventRepository
                .findTopBySessionSessionIdAndResumedAtIsNullOrderByPausedAtDesc(sessionId)
                .ifPresent(e -> e.resume(LocalDateTime.now()));

        return buildResponse(session);
    }

    // ── 세션 이탈 처리 ────────────────────────────────

    /**
     * 세션 이탈 처리
     * ACTIVE/PAUSED 세션을 ABANDONED로 변경한다.
     * 피드백이 없는 비정상 이탈이므로 태스크 진행률, 누적 시간, AI 통계는 갱신하지 않는다.
     */
    @Transactional
    public SessionResponse abandonSession(Long userId, Long sessionId) {
        FocusSession session = getSessionOrThrow(userId, sessionId);

        if (session.getSessionStatus() != FocusSession.SessionStatus.ACTIVE
                && session.getSessionStatus() != FocusSession.SessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        closeOpenPauseEvent(session.getSessionId());
        session.abandon();

        return buildResponse(session);
    }

    // ── 세션 종료 ─────────────────────────────────────

    /**
     * 세션 종료 + 피드백 저장
     *
     * 동작:
     * 1. 실제 소요 시간 계산 (일시정지 시간 제외)
     * 2. 세션 상태 ENDED 변경
     * 3. SessionFeedback 저장
     * 4. Task.progressPercent, totalTime 업데이트
     * 5. Python /sessions/estimate 호출
     *          → 잔여시간 재계산 결과를 task.remainingMin 업데이트
     *          → feedback.aiRemainingAfter 저장
     */
    @Transactional
    public SessionResponse endSession(Long userId, Long sessionId, SessionEndRequest request) {
        FocusSession session = getActiveSessionOrThrow(userId, sessionId);

        if (session.getSessionStatus() == FocusSession.SessionStatus.ENDED) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        // 실제 소요 시간 계산 (일시정지 시간 제외)
        closeOpenPauseEvent(session.getSessionId());
        int actualMinutes = calculateActualMinutes(session);
        session.end(actualMinutes);

        // 피드백 저장
        SessionFeedback feedback = sessionFeedbackRepository.save(
                SessionFeedback.builder()
                        .session(session)
                        .progressLevel(request.getProgressLevel())
                        .progressPercentAfter(request.getProgressPercentAfter())
                        .focusLevel(parseFocusLevel(request.getFocusLevel()))
                        .aiRemainingBefore(session.getAiRemainingBefore())
                        .note(request.getNote())
                        .build()
        );

        // 태스크 진행률 + 누적 시간 업데이트
        Task task = session.getTask();
        task.updateProgress(request.getProgressPercentAfter(), actualMinutes);
        if (session.getDailyPlanSession() != null) {
            session.getDailyPlanSession().markDone();
        }
        updateTaskTypeProfile(session, task, actualMinutes);
        applySessionAiEstimate(session, feedback, task, request.getProgressPercentAfter(), request.getFocusLevel());

        return buildResponse(session);
    }

    // ── 수동 기록 추가 ────────────────────────────────

    /**
     * 수동 학습 기록 추가
     * 타이머 없이 직접 시간 입력
     * source = MANUAL
     */
    @Transactional
    public SessionResponse addManualSession(Long userId, ManualSessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Task task = taskRepository.findByTaskIdAndUserUserId(request.getTaskId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        // 시작/종료 시각 검증
        if (!request.getEndedAt().isAfter(request.getStartedAt())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        int actualMinutes = (int) ChronoUnit.MINUTES.between(
                request.getStartedAt(), request.getEndedAt());

        FocusSession session = focusSessionRepository.save(FocusSession.builder()
                .task(task)
                .user(user)
                .source(FocusSession.SessionSource.MANUAL)
                .sessionStatus(FocusSession.SessionStatus.ENDED)
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .actualMinutes(actualMinutes)
                .aiRemainingBefore(task.getRemainingMin())
                .build());

        // 피드백 저장
        SessionFeedback feedback = sessionFeedbackRepository.save(SessionFeedback.builder()
                .session(session)
                .progressLevel(request.getProgressLevel())
                .progressPercentAfter(request.getProgressPercentAfter())
                .focusLevel(parseFocusLevel(request.getFocusLevel()))
                .aiRemainingBefore(task.getRemainingMin())
                .note(request.getNote())
                .build());

        // 태스크 진행률 + 누적 시간 업데이트
        task.updateProgress(request.getProgressPercentAfter(), actualMinutes);
        updateTaskTypeProfile(session, task, actualMinutes);
        applySessionAiEstimate(session, feedback, task, request.getProgressPercentAfter(), request.getFocusLevel());

        return buildResponse(session);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────

    private FocusSession getSessionOrThrow(Long userId, Long sessionId) {
        return focusSessionRepository.findBySessionIdAndUserUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void autoEndRunningSessions(Long userId) {
        focusSessionRepository.findAllByUserUserIdAndSessionStatusIn(
                        userId,
                        List.of(FocusSession.SessionStatus.ACTIVE, FocusSession.SessionStatus.PAUSED))
                .forEach(session -> {
                    closeOpenPauseEvent(session.getSessionId());
                    int actualMinutes = calculateActualMinutes(session);
                    session.end(actualMinutes);

                    Task task = session.getTask();
                    task.updateProgress(task.getProgressPercent(), actualMinutes);
                    if (session.getDailyPlanSession() != null) {
                        session.getDailyPlanSession().markDone();
                    }
                    updateTaskTypeProfile(session, task, actualMinutes);
                });
    }

    private FocusSession getActiveSessionOrThrow(Long userId, Long sessionId) {
        FocusSession session = getSessionOrThrow(userId, sessionId);
        if (session.getSessionStatus() == FocusSession.SessionStatus.ABANDONED) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        return session;
    }

    /**
     * 실제 소요 시간 계산
     * 전체 경과 시간 - 일시정지 총 시간
     */
    private int calculateActualMinutes(FocusSession session) {
        long totalElapsed = ChronoUnit.MINUTES.between(session.getStartedAt(), LocalDateTime.now());

        long pausedMinutes = sessionPauseEventRepository
                .findAllBySessionSessionIdOrderByPausedAtAsc(session.getSessionId())
                .stream()
                .mapToLong(e -> ChronoUnit.MINUTES.between(
                        e.getPausedAt(),
                        e.getResumedAt() != null ? e.getResumedAt() : LocalDateTime.now()))
                .sum();

        return (int) Math.max(1, totalElapsed - pausedMinutes);
    }

    private void closeOpenPauseEvent(Long sessionId) {
        sessionPauseEventRepository
                .findTopBySessionSessionIdAndResumedAtIsNullOrderByPausedAtDesc(sessionId)
                .ifPresent(event -> event.resume(LocalDateTime.now()));
    }

    private SessionFeedback.FocusLevel parseFocusLevel(String focusLevel) {
        try {
            return SessionFeedback.FocusLevel.valueOf(focusLevel);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void updateTaskTypeProfile(FocusSession session, Task task, int actualMinutes) {
        TaskType taskType = task.getTaskType();
        int plannedMinutes = resolvePlannedMinutes(session, task, actualMinutes);

        UserTaskTypeProfile profile = userTaskTypeProfileRepository
                .findByUserUserIdAndTaskTypeTaskTypeId(task.getUser().getUserId(), taskType.getTaskTypeId())
                .orElseGet(() -> userTaskTypeProfileRepository.save(UserTaskTypeProfile.builder()
                        .user(task.getUser())
                        .taskType(taskType)
                        .build()));
        profile.addSample(plannedMinutes, actualMinutes);
    }

    private void applySessionAiEstimate(
            FocusSession session,
            SessionFeedback feedback,
            Task task,
            Integer progressPercentAfter,
            String focusLevel
    ) {
        if (progressPercentAfter == null || progressPercentAfter <= 0) {
            feedback.applyAiEstimate(
                    resolvePreviousAiTotalMinutes(session, task),
                    resolvePreviousAiTotalMinutes(session, task),
                    task.getRemainingMin() != null ? task.getRemainingMin() : 0,
                    task.getRemainingMin() != null ? task.getRemainingMin() : 0,
                    0.0,
                    1.0,
                    task.getRemainingMin() != null ? task.getRemainingMin() : 0
            );
            return;
        }

        int previousAiTotalMinutes = resolvePreviousAiTotalMinutes(session, task);
        AiSessionEstimateResponse aiResponse = aiClient.estimateSession(new AiSessionEstimateRequest(
                task.getTotalTime(),
                progressPercentAfter / 100.0,
                focusLevel,
                previousAiTotalMinutes
        ));

        int updatedAiTotalMinutes = roundToInt(aiResponse.updatedAiTotalMinutes());
        int aiRemainingAfter = roundToInt(aiResponse.finalRemainingMinutes());
        feedback.applyAiEstimate(
                previousAiTotalMinutes,
                updatedAiTotalMinutes,
                roundToInt(aiResponse.progressBasedRemainingMinutes()),
                roundToInt(aiResponse.normalizedRemainingMinutes()),
                aiResponse.blendingWeight(),
                aiResponse.focusWeight(),
                aiRemainingAfter
        );
        task.applySessionAiEstimate(updatedAiTotalMinutes, aiRemainingAfter);
    }

    private int resolvePreviousAiTotalMinutes(FocusSession session, Task task) {
        int totalBeforeSession = Math.max(0, task.getTotalTime() - (session.getActualMinutes() != null
                ? session.getActualMinutes() : 0));
        if (session.getAiRemainingBefore() != null && session.getAiRemainingBefore() > 0) {
            return totalBeforeSession + session.getAiRemainingBefore();
        }
        if (task.getAiEstimated() != null && task.getAiEstimated() > 0) {
            return task.getAiEstimated();
        }
        if (task.getFinalEstimated() != null && task.getFinalEstimated() > 0) {
            return task.getFinalEstimated();
        }
        if (task.getUserEstimated() != null && task.getUserEstimated() > 0) {
            return task.getUserEstimated();
        }
        return Math.max(task.getTotalTime(), 1);
    }

    private int roundToInt(double value) {
        return Math.max(0, (int) Math.round(value));
    }

    private int resolvePlannedMinutes(FocusSession session, Task task, int actualMinutes) {
        if (session.getPlannedMinutes() != null && session.getPlannedMinutes() > 0) {
            return session.getPlannedMinutes();
        }
        if (task.getFinalEstimated() != null && task.getFinalEstimated() > 0) {
            return task.getFinalEstimated();
        }
        if (task.getUserEstimated() != null && task.getUserEstimated() > 0) {
            return task.getUserEstimated();
        }
        return actualMinutes;
    }

    private SessionResponse buildResponse(FocusSession session) {
        SessionFeedback feedback = sessionFeedbackRepository
                .findBySessionSessionId(session.getSessionId()).orElse(null);
        List<SessionPauseEvent> pauseEvents = sessionPauseEventRepository
                .findAllBySessionSessionIdOrderByPausedAtAsc(session.getSessionId());
        return SessionResponse.of(session, feedback, pauseEvents);
    }
}
