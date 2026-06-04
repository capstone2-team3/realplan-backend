package capstone2.team3.realplan.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "인증 토큰 응답")
public class AuthTokenResponse {

    @Schema(example = "Bearer")
    private final String tokenType;

    @Schema(example = "access-token")
    private final String accessToken;

    @Schema(example = "refresh-token")
    private final String refreshToken;

    @Schema(example = "1800000")
    private final long accessTokenExpiresIn;
}
