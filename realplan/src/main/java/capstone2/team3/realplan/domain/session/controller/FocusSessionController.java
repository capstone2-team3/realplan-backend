package capstone2.team3.realplan.domain.session.controller;

import capstone2.team3.realplan.domain.session.dto.*;
import capstone2.team3.realplan.domain.session.service.FocusSessionService;
import capstone2.team3.realplan.global.security.AuthUser;
import capstone2.team3.realplan.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FocusSession", description = "집중 세션 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequiredArgsConstructor
public class FocusSessionController {

    private final FocusSessionService focusSessionService;

    @Operation(summary = "태스크별 세션 목록 조회",
            description = "태스크의 전체 학습 기록 목록. 최신순 정렬")
    @GetMapping("/api/tasks/{taskId}/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.ok(
                focusSessionService.getSessions(authUser.getUserId(), taskId)));
    }

    @Operation(summary = "세션 시작",
            description = "집중 세션 시작. "
                    + "dailyPlanTaskId가 있으면 플랜에서 시작, 없으면 즉석 시작. "
                    + "같은 태스크에 이미 ACTIVE/PAUSED 세션이 있으면 400 반환")
    @PostMapping("/api/sessions")
    public ResponseEntity<ApiResponse<SessionResponse>> startSession(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SessionStartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        focusSessionService.startSession(authUser.getUserId(), request)));
    }

    @Operation(summary = "세션 일시정지",
            description = "ACTIVE → PAUSED. SessionPauseEvent 생성")
    @PatchMapping("/api/sessions/{sessionId}/pause")
    public ResponseEntity<ApiResponse<SessionResponse>> pauseSession(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                focusSessionService.pauseSession(authUser.getUserId(), sessionId)));
    }

    @Operation(summary = "세션 재개",
            description = "PAUSED → ACTIVE. 가장 최근 PauseEvent의 resumedAt 업데이트")
    @PatchMapping("/api/sessions/{sessionId}/resume")
    public ResponseEntity<ApiResponse<SessionResponse>> resumeSession(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                focusSessionService.resumeSession(authUser.getUserId(), sessionId)));
    }

    @Operation(summary = "세션 이탈 처리",
            description = "ACTIVE/PAUSED → ABANDONED. 열려 있는 일시정지 이벤트는 닫고 태스크 통계는 갱신하지 않습니다.")
    @PatchMapping("/api/sessions/{sessionId}/abandon")
    public ResponseEntity<ApiResponse<SessionResponse>> abandonSession(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                focusSessionService.abandonSession(authUser.getUserId(), sessionId)));
    }

    @Operation(summary = "세션 종료 + 피드백 저장",
            description = "세션 종료 후 피드백 저장. 일시정지 시간 제외한 실제 소요 시간 계산. "
                    + "태스크 progressPercent, totalTime 업데이트. "
                    + "TODO: Python /sessions/estimate 연동 예정")
    @PostMapping("/api/sessions/{sessionId}/end")
    public ResponseEntity<ApiResponse<SessionResponse>> endSession(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionEndRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                focusSessionService.endSession(authUser.getUserId(), sessionId, request)));
    }

    @Operation(summary = "수동 학습 기록 추가",
            description = "타이머 없이 직접 시간 입력. source=MANUAL로 저장. "
                    + "시작/종료 시각으로 실제 소요 시간 자동 계산")
    @PostMapping("/api/sessions/manual")
    public ResponseEntity<ApiResponse<SessionResponse>> addManualSession(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ManualSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        focusSessionService.addManualSession(authUser.getUserId(), request)));
    }
}
