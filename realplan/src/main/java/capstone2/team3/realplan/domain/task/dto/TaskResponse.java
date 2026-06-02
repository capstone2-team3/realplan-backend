package capstone2.team3.realplan.domain.task.dto;

import capstone2.team3.realplan.domain.task.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "태스크 응답")
public class TaskResponse {

    @Schema(example = "1")
    private final Long taskId;

    @Schema(example = "2")
    private final Long folderId;

    @Schema(example = "학교 과제")
    private final String folderName;

    @Schema(example = "1")
    private final Long taskTypeId;

    @Schema(example = "TIME_BASED")
    private final String taskTypeCode;

    @Schema(example = "시간형")
    private final String taskTypeNameKo;

    @Schema(example = "OS 챕터 7 문제 풀기")
    private final String name;

    @Schema(example = "문제집 p.142~p.180")
    private final String description;

    @Schema(example = "2026-06-10T23:59:00")
    private final LocalDateTime dueDate;

    @Schema(example = "HIGH")
    private final String importance;

    @Schema(example = "PENDING")
    private final String status;

    @Schema(example = "MEDIUM")
    private final String difficulty;

    @Schema(example = "true")
    private final boolean correctionEnabled;

    @Schema(example = "90")
    private final Integer userEstimated;

    @Schema(example = "100")
    private final Integer aiEstimated;

    @Schema(example = "100")
    private final Integer finalEstimated;

    @Schema(example = "100")
    private final Integer remainingMin;

    @Schema(example = "0")
    private final int progressPercent;

    @Schema(example = "0")
    private final int totalTime;

    @Schema(example = "2026-06-10T22:10:00")
    private final LocalDateTime completedAt;

    @Schema(example = "2026-06-10T21:30:00")
    private final LocalDateTime lastNotifiedAt;

    @Schema(example = "2026-06-03T22:30:00")
    private final LocalDateTime createdAt;

    @Schema(example = "2026-06-03T22:30:00")
    private final LocalDateTime updatedAt;

    private TaskResponse(Task task) {
        this.taskId = task.getTaskId();
        this.folderId = task.getFolder().getFolderId();
        this.folderName = task.getFolder().getName();
        this.taskTypeId = task.getTaskType().getTaskTypeId();
        this.taskTypeCode = task.getTaskType().getCode();
        this.taskTypeNameKo = task.getTaskType().getNameKo();
        this.name = task.getName();
        this.description = task.getDescription();
        this.dueDate = task.getDueDate();
        this.importance = task.getImportance().name();
        this.status = task.getStatus().name();
        this.difficulty = task.getDifficulty().name();
        this.correctionEnabled = task.isCorrectionEnabled();
        this.userEstimated = task.getUserEstimated();
        this.aiEstimated = task.getAiEstimated();
        this.finalEstimated = task.getFinalEstimated();
        this.remainingMin = task.getRemainingMin();
        this.progressPercent = task.getProgressPercent();
        this.totalTime = task.getTotalTime();
        this.completedAt = task.getCompletedAt();
        this.lastNotifiedAt = task.getLastNotifiedAt();
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
    }

    public static TaskResponse from(Task task) {
        return new TaskResponse(task);
    }
}
