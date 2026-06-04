package capstone2.team3.realplan.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "내 프로필 수정 요청")
public class UserProfileUpdateRequest {

    @Schema(example = "new@test.com")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    private String email;

    @Schema(example = "새닉네임")
    @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
    private String nickname;
}
