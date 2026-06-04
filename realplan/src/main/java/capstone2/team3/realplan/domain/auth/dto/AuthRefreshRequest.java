package capstone2.team3.realplan.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Access Token 재발급 요청")
public class AuthRefreshRequest {

    @Schema(example = "refresh-token")
    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}
