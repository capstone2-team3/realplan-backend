package capstone2.team3.realplan.domain.user.dto;

import capstone2.team3.realplan.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserProfileResponse {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private UserProfileResponse(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.lastLoginAt = user.getLastLoginAt();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user);
    }
}
