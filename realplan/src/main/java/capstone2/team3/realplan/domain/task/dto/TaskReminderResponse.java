package capstone2.team3.realplan.domain.task.dto;

import capstone2.team3.realplan.domain.task.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "태스크 리마인더 응답")
public class TaskReminderResponse {

    @Schema(example = "12")
    private final Long taskId;

    @Schema(example = "OS 과제")
    private final String name;

    @Schema(example = "2026-06-10T23:59:00")
    private final LocalDateTime dueDate;

    @Schema(example = "HIGH")
    private final String importance;

    @Schema(example = "IN_PROGRESS")
    private final String status;

    @Schema(example = "90")
    private final Integer remainingMin;

    @Schema(example = "40")
    private final int progressPercent;

    @Schema(example = "DUE_SOON")
    private final String reminderType;

    @Schema(example = "마감이 얼마 남지 않았어요")
    private final String message;

    @Schema(example = "92")
    private final int priority;

    @Schema(example = "2026-06-09T12:00:00")
    private final LocalDateTime lastNotifiedAt;

    private TaskReminderResponse(Task task, ReminderType reminderType, String message, int priority) {
        this.taskId = task.getTaskId();
        this.name = task.getName();
        this.dueDate = task.getDueDate();
        this.importance = task.getImportance().name();
        this.status = task.getStatus().name();
        this.remainingMin = task.getRemainingMin();
        this.progressPercent = task.getProgressPercent();
        this.reminderType = reminderType.name();
        this.message = message;
        this.priority = priority;
        this.lastNotifiedAt = task.getLastNotifiedAt();
    }

    public static TaskReminderResponse of(Task task, ReminderType reminderType, String message, int priority) {
        return new TaskReminderResponse(task, reminderType, message, priority);
    }

    public enum ReminderType {
        OVERDUE,
        DUE_TODAY,
        DUE_SOON,
        IN_PROGRESS,
        HIGH_IMPORTANCE,
        UPCOMING
    }
}
