package capstone2.team3.realplan.global.security;

import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtil {

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenExpiryMillis;
    private final long refreshTokenExpiryMillis;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiryMillis,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiryMillis) {
        this.objectMapper = new ObjectMapper();
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpiryMillis = accessTokenExpiryMillis;
        this.refreshTokenExpiryMillis = refreshTokenExpiryMillis;
    }

    public String createAccessToken(Long userId, String email) {
        return createToken(userId, email, ACCESS_TOKEN_TYPE, accessTokenExpiryMillis);
    }

    public String createRefreshToken(Long userId, String email) {
        return createToken(userId, email, REFRESH_TOKEN_TYPE, refreshTokenExpiryMillis);
    }

    public long getAccessTokenExpiryMillis() {
        return accessTokenExpiryMillis;
    }

    public JwtClaims parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            Map<String, Object> payload = objectMapper.readValue(
                    base64UrlDecode(parts[1]),
                    new TypeReference<>() {
                    }
            );

            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
            }

            Long userId = ((Number) payload.get("userId")).longValue();
            String email = (String) payload.get("email");
            String tokenType = (String) payload.get("typ");
            return new JwtClaims(userId, email, tokenType, expiresAt);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public void validateTokenType(JwtClaims claims, String expectedTokenType) {
        if (!expectedTokenType.equals(claims.tokenType())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String createToken(Long userId, String email, String tokenType, long expiryMillis) {
        try {
            long now = Instant.now().getEpochSecond();
            long expiresAt = now + expiryMillis / 1000;

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("userId", userId);
            payload.put("email", email);
            payload.put("typ", tokenType);
            payload.put("iat", now);
            payload.put("exp", expiresAt);

            String unsignedToken = base64UrlEncode(objectMapper.writeValueAsBytes(header))
                    + "."
                    + base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
