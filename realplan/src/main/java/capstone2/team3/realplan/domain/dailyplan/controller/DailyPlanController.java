package capstone2.team3.realplan.domain.dailyplan.controller;

import capstone2.team3.realplan.domain.dailyplan.dto.*;
import capstone2.team3.realplan.domain.dailyplan.service.DailyPlanService;
import capstone2.team3.realplan.global.security.AuthUser;
import capstone2.team3.realplan.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "DailyPlan", description = "데일리 플랜 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/daily-plans")
@RequiredArgsConstructor
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    @Operation(summary = "날짜별 플랜 조회",
            description = "슬롯(태스크 배정 정보 포함) + 플랜 태스크 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<DailyPlanResponse>> getPlan(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.getPlan(authUser.getUserId(), date)));
    }

    @Operation(summary = "플랜 생성",
            description = "1단계: 가용시간 슬롯 선택 후 플랜 생성. 태스크 배정은 이후 단계")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyPlanResponse>> createPlan(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody DailyPlanCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyPlanService.createPlan(authUser.getUserId(), request)));
    }

    @Operation(summary = "AI 태스크 추천",
            description = "2단계: 미완료 태스크 추천 순위 조회. DB 저장 없음")
    @GetMapping("/{planId}/recommend")
    public ResponseEntity<ApiResponse<RecommendResponse>> getRecommendations(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long planId) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.getRecommendations(authUser.getUserId(), planId)));
    }

    @Operation(summary = "슬롯 단건 배정 (사용자 직접)",
            description = "3단계-A: 슬롯 하나에 태스크 직접 배정. "
                    + "태스크가 플랜에 없으면 자동 추가됨. "
                    + "taskId=null이면 배정 해제")
    @PatchMapping("/{planId}/slots/{slotId}")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> assignTaskToSlot(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long planId,
            @PathVariable Long slotId,
            @RequestBody SlotAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.assignTaskToSlot(authUser.getUserId(), planId, slotId, request)));
    }

    @Operation(summary = "슬롯 일괄 배정 (AI 자동배치 결과 적용)",
            description = "3단계-B: AI /schedules/auto-place 결과를 슬롯에 일괄 배정. "
                    + "기존 AI 배정은 초기화 후 새로 적용. "
                    + "slotIndexes 배열로 슬롯 지정")
    @PutMapping("/{planId}/slots/batch")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> batchAssignTasks(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long planId,
            @Valid @RequestBody SlotBatchAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.batchAssignTasks(authUser.getUserId(), planId, request)));
    }

    @Operation(summary = "플랜 상태 변경",
            description = "4단계: CONFIRMED = 오늘의 플랜 확정 / REJECTED = 거절")
    @PatchMapping("/{planId}")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> updatePlanStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long planId,
            @Valid @RequestBody DailyPlanStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.updatePlanStatus(authUser.getUserId(), planId, request)));
    }

    @Operation(summary = "가용시간 슬롯 전체 교체",
            description = "슬롯 배열로 전체 교체. availableMinutes 자동 재계산")
    @PutMapping("/{planId}/slots")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> updateSlots(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long planId,
            @Valid @RequestBody SlotUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.updateSlots(authUser.getUserId(), planId, request)));
    }

    @Operation(summary = "플랜 태스크 수정",
            description = "플랜 내 태스크 선택/해제 또는 순서 변경")
    @PatchMapping("/{planId}/tasks/{dailyPlanTaskId}")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> updatePlanTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long planId,
            @PathVariable Long dailyPlanTaskId,
            @RequestBody DailyPlanTaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPlanService.updatePlanTask(authUser.getUserId(), planId, dailyPlanTaskId, request)));
    }
}
