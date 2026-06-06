package capstone2.team3.realplan.domain.user.service;

import capstone2.team3.realplan.domain.folder.entity.Folder;
import capstone2.team3.realplan.domain.folder.repository.FolderRepository;
import capstone2.team3.realplan.domain.auth.repository.RefreshTokenRepository;
import capstone2.team3.realplan.domain.user.dto.UserProfileResponse;
import capstone2.team3.realplan.domain.user.dto.UserProfileUpdateRequest;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(getUserOrThrow(userId));
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getUserOrThrow(userId);

        String passwordHash = request.getPassword() == null ? null : passwordEncoder.encode(request.getPassword());
        user.updateProfile(request.getNickname(), passwordHash);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void resetMyData(Long userId) {
        User user = getUserOrThrow(userId);
        deleteUserData(userId);

        folderRepository.save(Folder.builder()
                .user(user)
                .name("기본")
                .isDefault(true)
                .build());
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getUserOrThrow(userId);

        refreshTokenRepository.deleteAllByUserUserId(userId);
        deleteUserData(userId);
        userRepository.delete(user);
    }

    private void deleteUserData(Long userId) {
        executeDelete("DELETE FROM ai_coefficient_update_log WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM ai_estimation_log WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM user_ai_folder_residual WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM user_ai_difficulty_residual WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM user_ai_type_residual WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM user_ai_profile WHERE user_id = :userId", userId);

        executeDelete("DELETE FROM session_feedback WHERE session_id IN "
                + "(SELECT session_id FROM focus_session WHERE user_id = :userId)", userId);
        executeDelete("DELETE FROM session_pause_event WHERE session_id IN "
                + "(SELECT session_id FROM focus_session WHERE user_id = :userId)", userId);
        executeDelete("DELETE FROM focus_session WHERE user_id = :userId", userId);

        executeDelete("DELETE FROM daily_plan_session_block WHERE daily_plan_session_id IN "
                + "(SELECT daily_plan_session_id FROM daily_plan_session WHERE user_id = :userId)", userId);
        executeDelete("DELETE FROM daily_plan_session WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM daily_plan_slot WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM daily_plan_task WHERE daily_plan_id IN "
                + "(SELECT daily_plan_id FROM daily_plan WHERE user_id = :userId)", userId);
        executeDelete("DELETE FROM daily_plan WHERE user_id = :userId", userId);

        executeDelete("DELETE FROM user_task_type_profile WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM task WHERE user_id = :userId", userId);
        executeDelete("DELETE FROM folder WHERE user_id = :userId", userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void executeDelete(String sql, Long userId) {
        entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
