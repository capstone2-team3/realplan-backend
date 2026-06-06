package capstone2.team3.realplan.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "내 프로필 수정 요청")
public class UserProfileUpdateRequest {

    @Schema(example = "새닉네임")
    @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
    private String nickname;

    @Schema(example = "newpassword1234")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    private String password;
}
