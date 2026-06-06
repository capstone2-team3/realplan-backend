package capstone2.team3.realplan.domain.user.controller;

import capstone2.team3.realplan.domain.user.dto.UserProfileResponse;
import capstone2.team3.realplan.domain.user.dto.UserProfileUpdateRequest;
import capstone2.team3.realplan.domain.user.service.UserService;
import capstone2.team3.realplan.global.common.ApiResponse;
import capstone2.team3.realplan.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(authUser.getUserId())));
    }

    @Operation(summary = "내 프로필 수정", description = "nickname, password를 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateMyProfile(authUser.getUserId(), request)));
    }

    @Operation(summary = "내 전체 데이터 초기화", description = "계정은 유지하고 태스크, 폴더, 플랜, 세션 데이터를 초기화합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> resetMyData(
            @AuthenticationPrincipal AuthUser authUser) {
        userService.resetMyData(authUser.getUserId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "회원탈퇴", description = "내 전체 데이터와 계정을 삭제합니다.")
    @DeleteMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal AuthUser authUser) {
        userService.withdraw(authUser.getUserId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
