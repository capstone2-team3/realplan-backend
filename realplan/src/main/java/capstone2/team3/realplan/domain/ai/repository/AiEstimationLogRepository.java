package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.AiEstimationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiEstimationLogRepository extends JpaRepository<AiEstimationLog, Long> {

    List<AiEstimationLog> findAllByTaskTaskIdOrderByCreatedAtDesc(Long taskId);
}
