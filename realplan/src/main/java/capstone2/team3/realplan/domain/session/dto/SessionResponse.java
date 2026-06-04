package capstone2.team3.realplan.domain.session.dto;

import capstone2.team3.realplan.domain.session.entity.FocusSession;
import capstone2.team3.realplan.domain.session.entity.SessionFeedback;
import capstone2.team3.realplan.domain.session.entity.SessionPauseEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class SessionResponse {

    private final Long sessionId;
    private final Long taskId;
    private final String taskName;
    private final Long dailyPlanTaskId;
    private final Long dailyPlanSessionId;
    private final String source;
    private final String sessionStatus;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final Integer actualMinutes;
    private final Integer plannedMinutes;
    private final Integer aiRemainingBefore;
    private final FeedbackResponse feedback;
    private final List<PauseEventResponse> pauseEvents;
    private final LocalDateTime createdAt;

    private SessionResponse(FocusSession session,
                            SessionFeedback feedback,
                            List<SessionPauseEvent> pauseEvents) {
        this.sessionId = session.getSessionId();
        this.taskId = session.getTask().getTaskId();
        this.taskName = session.getTask().getName();
        this.dailyPlanTaskId = session.getDailyPlanTask() != null
                ? session.getDailyPlanTask().getDailyPlanTaskId() : null;
        this.dailyPlanSessionId = session.getDailyPlanSession() != null
                ? session.getDailyPlanSession().getDailyPlanSessionId() : null;
        this.source = session.getSource().name();
        this.sessionStatus = session.getSessionStatus().name();
        this.startedAt = session.getStartedAt();
        this.endedAt = session.getEndedAt();
        this.actualMinutes = session.getActualMinutes();
        this.plannedMinutes = session.getPlannedMinutes();
        this.aiRemainingBefore = session.getAiRemainingBefore();
        this.feedback = feedback != null ? FeedbackResponse.from(feedback) : null;
        this.pauseEvents = pauseEvents.stream().map(PauseEventResponse::from).toList();
        this.createdAt = session.getCreatedAt();
    }

    public static SessionResponse of(FocusSession session,
                                     SessionFeedback feedback,
                                     List<SessionPauseEvent> pauseEvents) {
        return new SessionResponse(session, feedback, pauseEvents);
    }

    // ── 피드백 응답 ──────────────────────────────────

    @Getter
    public static class FeedbackResponse {
        private final Long feedbackId;
        private final int progressLevel;
        private final Integer progressPercentAfter;
        private final String focusLevel;
        private final Integer aiRemainingBefore;
        private final Integer aiRemainingAfter;
        private final Integer previousAiTotalMinutes;
        private final Integer updatedAiTotalMinutes;
        private final Integer progressBasedRemainingMinutes;
        private final Integer normalizedRemainingMinutes;
        private final java.math.BigDecimal blendingWeight;
        private final java.math.BigDecimal focusWeight;
        private final String note;

        private FeedbackResponse(SessionFeedback feedback) {
            this.feedbackId = feedback.getFeedbackId();
            this.progressLevel = feedback.getProgressLevel();
            this.progressPercentAfter = feedback.getProgressPercentAfter();
            this.focusLevel = feedback.getFocusLevel().name();
            this.aiRemainingBefore = feedback.getAiRemainingBefore();
            this.aiRemainingAfter = feedback.getAiRemainingAfter();
            this.previousAiTotalMinutes = feedback.getPreviousAiTotalMinutes();
            this.updatedAiTotalMinutes = feedback.getUpdatedAiTotalMinutes();
            this.progressBasedRemainingMinutes = feedback.getProgressBasedRemainingMinutes();
            this.normalizedRemainingMinutes = feedback.getNormalizedRemainingMinutes();
            this.blendingWeight = feedback.getBlendingWeight();
            this.focusWeight = feedback.getFocusWeight();
            this.note = feedback.getNote();
        }

        public static FeedbackResponse from(SessionFeedback feedback) {
            return new FeedbackResponse(feedback);
        }
    }

    // ── 일시정지 이벤트 응답 ──────────────────────────

    @Getter
    public static class PauseEventResponse {
        private final Long pauseEventId;
        private final LocalDateTime pausedAt;
        private final LocalDateTime resumedAt;

        private PauseEventResponse(SessionPauseEvent event) {
            this.pauseEventId = event.getPauseEventId();
            this.pausedAt = event.getPausedAt();
            this.resumedAt = event.getResumedAt();
        }

        public static PauseEventResponse from(SessionPauseEvent event) {
            return new PauseEventResponse(event);
        }
    }
}
