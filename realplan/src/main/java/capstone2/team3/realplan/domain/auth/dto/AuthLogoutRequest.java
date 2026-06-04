package capstone2.team3.realplan.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그아웃 요청")
public class AuthLogoutRequest {

    @Schema(example = "refresh-token")
    private String refreshToken;
}
