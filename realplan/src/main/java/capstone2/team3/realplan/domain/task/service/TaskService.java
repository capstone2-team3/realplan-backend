package capstone2.team3.realplan.domain.task.service;

import capstone2.team3.realplan.domain.ai.client.AiClient;
import capstone2.team3.realplan.domain.ai.dto.AiTaskClassifyRequest;
import capstone2.team3.realplan.domain.ai.dto.AiTaskClassifyResponse;
import capstone2.team3.realplan.domain.ai.service.TaskAiEstimationService;
import capstone2.team3.realplan.domain.ai.service.TaskAiCoefficientUpdateService;
import capstone2.team3.realplan.domain.folder.entity.Folder;
import capstone2.team3.realplan.domain.folder.repository.FolderRepository;
import capstone2.team3.realplan.domain.task.dto.TaskClassifyRequest;
import capstone2.team3.realplan.domain.task.dto.TaskClassifyResponse;
import capstone2.team3.realplan.domain.task.dto.TaskCreateRequest;
import capstone2.team3.realplan.domain.task.dto.TaskResponse;
import capstone2.team3.realplan.domain.task.dto.TaskUpdateRequest;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.domain.task.repository.TaskRepository;
import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import capstone2.team3.realplan.domain.tasktype.repository.TaskTypeRepository;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private static final int CLASSIFY_HISTORY_LIMIT = 50;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final AiClient aiClient;
    private final TaskAiEstimationService taskAiEstimationService;
    private final TaskAiCoefficientUpdateService taskAiCoefficientUpdateService;

    // 태스크 목록 조회
    public List<TaskResponse> getTasks(Long userId, Long folderId, String filter, String sort) {
        List<Task> tasks;

        if (folderId != null) {
            // 폴더 소유자 검증
            folderRepository.findByFolderIdAndUserUserId(folderId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
            tasks = taskRepository.findAllByFolderFolderId(folderId);
        } else {
            tasks = taskRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId);
        }

        // 필터
        if ("ACTIVE".equals(filter)) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus() != Task.Status.COMPLETED)
                    .toList();
        } else if ("COMPLETED".equals(filter)) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus() == Task.Status.COMPLETED)
                    .toList();
        }

        // 정렬
        tasks = switch (sort == null ? "RECENT" : sort) {
            case "DEADLINE" -> tasks.stream()
                    .filter(t -> t.getDueDate() != null)
                    .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                    .toList();
            case "IMPORTANCE" -> tasks.stream()
                    .sorted((a, b) -> b.getImportance().compareTo(a.getImportance()))
                    .toList();
            default -> tasks; // RECENT, CREATED
        };

        return tasks.stream().map(TaskResponse::from).toList();
    }

    // 태스크 상세 조회
    public TaskResponse getTask(Long userId, Long taskId) {
        Task task = getTaskOrThrow(userId, taskId);
        return TaskResponse.from(task);
    }

    // 태스크 유형 추천. DB 저장 없이 AI 분류 결과만 반환한다.
    public TaskClassifyResponse classifyTask(Long userId, TaskClassifyRequest request) {
        List<AiTaskClassifyRequest.HistoryItem> userHistory = resolveClassifyHistory(userId, request);
        AiTaskClassifyResponse response = aiClient.classifyTask(new AiTaskClassifyRequest(
                request.name(),
                userHistory
        ));
        TaskType taskType = taskTypeRepository.findByCode(response.taskType())
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_TYPE_NOT_FOUND));
        return TaskClassifyResponse.of(response, taskType);
    }

    // 태스크 생성
    @Transactional
    public TaskResponse createTask(Long userId, TaskCreateRequest request) {
        validateEstimatedMinutes(request.getUserEstimated());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Folder folder = folderRepository.findByFolderIdAndUserUserId(request.getFolderId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

        TaskType taskType = taskTypeRepository.findById(request.getTaskTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_TYPE_NOT_FOUND));

        Task task = Task.builder()
                .user(user)
                .folder(folder)
                .taskType(taskType)
                .name(request.getName())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .importance(Task.Importance.valueOf(request.getImportance()))
                .difficulty(Task.Difficulty.valueOf(request.getDifficulty()))
                .correctionEnabled(request.isCorrectionEnabled())
                .userEstimated(request.getUserEstimated())
                .finalEstimated(request.getUserEstimated())
                .remainingMin(request.getUserEstimated())
                .build();

        Task savedTask = taskRepository.save(task);
        taskAiEstimationService.estimateAndApply(savedTask);
        return TaskResponse.from(savedTask);
    }

    // 태스크 수정
    @Transactional
    public TaskResponse updateTask(Long userId, Long taskId, TaskUpdateRequest request) {
        Task task = getTaskOrThrow(userId, taskId);
        if (request.getUserEstimated() != null) {
            validateEstimatedMinutes(request.getUserEstimated());
        }

        // 폴더 변경
        if (request.getFolderId() != null) {
            Folder newFolder = folderRepository.findByFolderIdAndUserUserId(request.getFolderId(), userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
            task.updateFolder(newFolder);
        }

        if (request.getTaskTypeId() != null) {
            TaskType newTaskType = taskTypeRepository.findById(request.getTaskTypeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TASK_TYPE_NOT_FOUND));
            task.updateTaskType(newTaskType);
        }

        // 나머지 필드는 리플렉션 없이 직접 처리
        // Task Entity에 updateInfo 메서드 추가 방식으로 처리
        task.updateInfo(
                request.getName(),
                request.getDescription(),
                request.getDueDate(),
                request.getImportance() != null ? Task.Importance.valueOf(request.getImportance()) : null,
                request.getDifficulty() != null ? Task.Difficulty.valueOf(request.getDifficulty()) : null,
                request.getCorrectionEnabled(),
                request.getUserEstimated()
        );
        taskAiEstimationService.estimateAndApply(task);

        return TaskResponse.from(task);
    }

    // 태스크 삭제
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = getTaskOrThrow(userId, taskId);
        taskRepository.delete(task);
    }

    // 태스크 완료 처리
    @Transactional
    public TaskResponse completeTask(Long userId, Long taskId) {
        Task task = getTaskOrThrow(userId, taskId);
        taskAiCoefficientUpdateService.updateOnTaskCompleted(task);
        task.complete();
        return TaskResponse.from(task);
    }

    // ── 내부 헬퍼 ────────────────────────────────────

    private Task getTaskOrThrow(Long userId, Long taskId) {
        return taskRepository.findByTaskIdAndUserUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private void validateEstimatedMinutes(Integer estimatedMinutes) {
        if (estimatedMinutes == null || estimatedMinutes <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private List<AiTaskClassifyRequest.HistoryItem> resolveClassifyHistory(
            Long userId,
            TaskClassifyRequest request
    ) {
        if (request.userHistory() != null) {
            return request.userHistory().stream()
                    .map(item -> new AiTaskClassifyRequest.HistoryItem(item.name(), item.taskType()))
                    .toList();
        }

        return taskRepository.findAllByUserUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(0, CLASSIFY_HISTORY_LIMIT)
                ).stream()
                .map(task -> new AiTaskClassifyRequest.HistoryItem(
                        task.getName(),
                        task.getTaskType().getCode()
                ))
                .toList();
    }
}
