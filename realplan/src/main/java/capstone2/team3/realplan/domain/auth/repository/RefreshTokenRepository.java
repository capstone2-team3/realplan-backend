package capstone2.team3.realplan.domain.auth.repository;

import capstone2.team3.realplan.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteAllByUserUserId(Long userId);
}
