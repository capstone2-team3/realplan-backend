package capstone2.team3.realplan.domain.task.controller;

import capstone2.team3.realplan.domain.task.dto.TaskCreateRequest;
import capstone2.team3.realplan.domain.task.dto.TaskResponse;
import capstone2.team3.realplan.domain.task.dto.TaskUpdateRequest;
import capstone2.team3.realplan.domain.task.service.TaskService;
import capstone2.team3.realplan.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Task", description = "태스크 API")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // TODO: JWT 구현 후 @AuthenticationPrincipal로 교체 예정
    private static final Long TEMP_USER_ID = 1L;

    @Operation(summary = "태스크 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false, defaultValue = "ALL") String filter,
            @RequestParam(required = false, defaultValue = "RECENT") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.getTasks(TEMP_USER_ID, folderId, filter, sort)));
    }

    @Operation(summary = "태스크 상세 조회")
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getTask(TEMP_USER_ID, taskId)));
    }

    @Operation(summary = "태스크 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(taskService.createTask(TEMP_USER_ID, request)));
    }

    @Operation(summary = "태스크 수정 (부분 업데이트)")
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.updateTask(TEMP_USER_ID, taskId, request)));
    }

    @Operation(summary = "태스크 삭제")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(TEMP_USER_ID, taskId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "태스크 완료 처리")
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.completeTask(TEMP_USER_ID, taskId)));
    }
}