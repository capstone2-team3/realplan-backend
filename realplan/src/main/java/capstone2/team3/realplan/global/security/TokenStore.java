package capstone2.team3.realplan.global.security;

import capstone2.team3.realplan.domain.auth.entity.InvalidatedAccessToken;
import capstone2.team3.realplan.domain.auth.entity.RefreshToken;
import capstone2.team3.realplan.domain.auth.repository.InvalidatedAccessTokenRepository;
import capstone2.team3.realplan.domain.auth.repository.RefreshTokenRepository;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenStore {

    private final RefreshTokenRepository refreshTokenRepository;
    private final InvalidatedAccessTokenRepository invalidatedAccessTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveRefreshToken(String refreshToken, Long userId, long expiresAtEpochSecond) {
        deleteExpiredTokens();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash(refreshToken))
                .user(user)
                .expiresAt(toLocalDateTime(expiresAtEpochSecond))
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(entity);
    }

    public Long getRefreshTokenUserId(String refreshToken) {
        return refreshTokenRepository.findById(hash(refreshToken))
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(token -> token.getUser().getUserId())
                .orElse(null);
    }

    @Transactional
    public void removeRefreshToken(String refreshToken) {
        refreshTokenRepository.deleteById(hash(refreshToken));
    }

    @Transactional
    public void invalidateAccessToken(String accessToken, long expiresAtEpochSecond) {
        deleteExpiredTokens();

        InvalidatedAccessToken entity = InvalidatedAccessToken.builder()
                .tokenHash(hash(accessToken))
                .expiresAt(toLocalDateTime(expiresAtEpochSecond))
                .createdAt(LocalDateTime.now())
                .build();
        invalidatedAccessTokenRepository.save(entity);
    }

    public boolean isAccessTokenInvalidated(String accessToken) {
        return invalidatedAccessTokenRepository.findById(hash(accessToken))
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void deleteExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        invalidatedAccessTokenRepository.deleteByExpiresAtBefore(now);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private LocalDateTime toLocalDateTime(long epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }
}
