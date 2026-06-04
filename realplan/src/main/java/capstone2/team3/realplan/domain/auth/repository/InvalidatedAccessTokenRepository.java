package capstone2.team3.realplan.domain.auth.repository;

import capstone2.team3.realplan.domain.auth.entity.InvalidatedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InvalidatedAccessTokenRepository extends JpaRepository<InvalidatedAccessToken, String> {

    void deleteByExpiresAtBefore(LocalDateTime now);
}
