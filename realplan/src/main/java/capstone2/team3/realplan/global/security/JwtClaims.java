package capstone2.team3.realplan.global.security;

public record JwtClaims(
        Long userId,
        String email,
        String tokenType,
        long expiresAt
) {
}
