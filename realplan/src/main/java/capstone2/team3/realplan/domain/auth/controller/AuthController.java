package capstone2.team3.realplan.domain.auth.controller;

import capstone2.team3.realplan.domain.auth.dto.AuthLoginRequest;
import capstone2.team3.realplan.domain.auth.dto.AuthLogoutRequest;
import capstone2.team3.realplan.domain.auth.dto.AuthRefreshRequest;
import capstone2.team3.realplan.domain.auth.dto.AuthRegisterRequest;
import capstone2.team3.realplan.domain.auth.dto.AuthTokenResponse;
import capstone2.team3.realplan.domain.auth.service.AuthService;
import capstone2.team3.realplan.global.common.ApiResponse;
import capstone2.team3.realplan.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(
            @Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(request)));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody AuthRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.getRefreshToken())));
    }

    @Operation(summary = "로그아웃")
    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody(required = false) AuthLogoutRequest request) {
        String accessToken = authorizationHeader.startsWith(BEARER_PREFIX)
                ? authorizationHeader.substring(BEARER_PREFIX.length())
                : authorizationHeader;
        authService.logout(accessToken, request != null ? request.getRefreshToken() : null);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
