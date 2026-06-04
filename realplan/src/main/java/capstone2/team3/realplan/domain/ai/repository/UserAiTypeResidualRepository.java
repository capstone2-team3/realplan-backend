package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.UserAiTypeResidual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAiTypeResidualRepository extends JpaRepository<UserAiTypeResidual, Long> {

    List<UserAiTypeResidual> findAllByUserUserId(Long userId);

    Optional<UserAiTypeResidual> findByUserUserIdAndTaskTypeTaskTypeId(Long userId, Long taskTypeId);
}
