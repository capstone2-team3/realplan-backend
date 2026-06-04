package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.AiCoefficientUpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiCoefficientUpdateLogRepository extends JpaRepository<AiCoefficientUpdateLog, Long> {

    List<AiCoefficientUpdateLog> findAllByTaskTaskIdOrderByCreatedAtDesc(Long taskId);
}
