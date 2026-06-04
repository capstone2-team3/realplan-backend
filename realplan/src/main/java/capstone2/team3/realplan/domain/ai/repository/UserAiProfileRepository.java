package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.UserAiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAiProfileRepository extends JpaRepository<UserAiProfile, Long> {

    Optional<UserAiProfile> findByUserUserId(Long userId);
}
