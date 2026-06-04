package capstone2.team3.realplan.domain.auth.service;

import capstone2.team3.realplan.domain.auth.dto.AuthLoginRequest;
import capstone2.team3.realplan.domain.auth.dto.AuthRegisterRequest;
import capstone2.team3.realplan.domain.auth.dto.AuthTokenResponse;
import capstone2.team3.realplan.domain.folder.entity.Folder;
import capstone2.team3.realplan.domain.folder.repository.FolderRepository;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import capstone2.team3.realplan.global.security.JwtClaims;
import capstone2.team3.realplan.global.security.JwtUtil;
import capstone2.team3.realplan.global.security.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String DEFAULT_FOLDER_NAME = "기본 폴더";

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenStore tokenStore;

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);
        createDefaultFolder(savedUser);
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        user.updateLastLoginAt();
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        JwtClaims claims = jwtUtil.parseToken(refreshToken);
        jwtUtil.validateTokenType(claims, JwtUtil.REFRESH_TOKEN_TYPE);

        Long storedUserId = tokenStore.getRefreshTokenUserId(refreshToken);
        if (storedUserId == null || !storedUserId.equals(claims.userId())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        tokenStore.removeRefreshToken(refreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            JwtClaims accessClaims = jwtUtil.parseToken(accessToken);
            jwtUtil.validateTokenType(accessClaims, JwtUtil.ACCESS_TOKEN_TYPE);
            tokenStore.invalidateAccessToken(accessToken, accessClaims.expiresAt());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenStore.removeRefreshToken(refreshToken);
        }
    }

    private AuthTokenResponse issueTokens(User user) {
        String accessToken = jwtUtil.createAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getUserId(), user.getEmail());
        JwtClaims refreshClaims = jwtUtil.parseToken(refreshToken);
        tokenStore.saveRefreshToken(refreshToken, user.getUserId(), refreshClaims.expiresAt());
        return new AuthTokenResponse("Bearer", accessToken, refreshToken, jwtUtil.getAccessTokenExpiryMillis());
    }

    private void createDefaultFolder(User user) {
        Folder folder = Folder.builder()
                .user(user)
                .name(DEFAULT_FOLDER_NAME)
                .isDefault(true)
                .build();
        folderRepository.save(folder);
    }
}
