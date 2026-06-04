package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.AiSystemPrior;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiSystemPriorRepository extends JpaRepository<AiSystemPrior, Long> {

    Optional<AiSystemPrior> findFirstByIsActiveTrueOrderByCreatedAtDesc();
}
