package capstone2.team3.realplan.domain.task.controller;

import capstone2.team3.realplan.domain.task.dto.TaskClassifyRequest;
import capstone2.team3.realplan.domain.task.dto.TaskClassifyResponse;
import capstone2.team3.realplan.domain.task.dto.TaskCreateRequest;
import capstone2.team3.realplan.domain.task.dto.TaskResponse;
import capstone2.team3.realplan.domain.task.dto.TaskUpdateRequest;
import capstone2.team3.realplan.domain.task.service.TaskService;
import capstone2.team3.realplan.global.common.ApiResponse;
import capstone2.team3.realplan.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Task", description = "태스크 API")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "태스크 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false, defaultValue = "ALL") String filter,
            @RequestParam(required = false, defaultValue = "RECENT") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.getTasks(authUser.getUserId(), folderId, filter, sort)));
    }

    @Operation(summary = "태스크 상세 조회")
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getTask(authUser.getUserId(), taskId)));
    }

    @Operation(summary = "태스크 유형 추천", description = "태스크 이름 기반 유형 추천. DB 저장 없이 추천 결과만 반환합니다.")
    @PostMapping("/classify")
    public ResponseEntity<ApiResponse<TaskClassifyResponse>> classifyTask(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TaskClassifyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.classifyTask(authUser.getUserId(), request)));
    }

    @Operation(summary = "태스크 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(taskService.createTask(authUser.getUserId(), request)));
    }

    @Operation(summary = "태스크 수정 (부분 업데이트)")
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.updateTask(authUser.getUserId(), taskId, request)));
    }

    @Operation(summary = "태스크 삭제")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long taskId) {
        taskService.deleteTask(authUser.getUserId(), taskId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "태스크 완료 처리")
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.completeTask(authUser.getUserId(), taskId)));
    }
}
