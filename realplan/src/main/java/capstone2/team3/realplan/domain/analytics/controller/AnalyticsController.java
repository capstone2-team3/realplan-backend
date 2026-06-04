package capstone2.team3.realplan.domain.analytics.controller;

import capstone2.team3.realplan.domain.analytics.dto.DailyStudyTimeResponse;
import capstone2.team3.realplan.domain.analytics.dto.FocusByHourResponse;
import capstone2.team3.realplan.domain.analytics.dto.TypeStatsResponse;
import capstone2.team3.realplan.domain.analytics.dto.WeeklyAnalyticsResponse;
import capstone2.team3.realplan.domain.analytics.service.AnalyticsService;
import capstone2.team3.realplan.global.common.ApiResponse;
import capstone2.team3.realplan.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "통계 API")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "주간 통계", description = "총 시간, 평균 집중도, 완료 태스크 수와 전주 대비 값을 조회합니다.")
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyAnalyticsResponse>> getWeekly(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getWeekly(authUser.getUserId())));
    }

    @Operation(summary = "일별 학습 시간", description = "막대 그래프용 일별 학습 시간을 조회합니다.")
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyStudyTimeResponse>> getDaily(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "2") int weeks) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getDaily(authUser.getUserId(), weeks)));
    }

    @Operation(summary = "시간대별 평균 집중도", description = "최근 4주 기준 2시간 단위 평균 집중도를 조회합니다.")
    @GetMapping("/focus-by-hour")
    public ResponseEntity<ApiResponse<FocusByHourResponse>> getFocusByHour(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getFocusByHour(authUser.getUserId())));
    }

    @Operation(summary = "타입별 통계", description = "UserTaskTypeProfile 기반 예상 vs 실제 시간과 보정 계수를 조회합니다.")
    @GetMapping("/type-stats")
    public ResponseEntity<ApiResponse<TypeStatsResponse>> getTypeStats(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getTypeStats(authUser.getUserId())));
    }
}
