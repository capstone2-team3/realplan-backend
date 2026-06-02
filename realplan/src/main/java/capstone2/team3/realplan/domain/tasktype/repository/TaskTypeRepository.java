package capstone2.team3.realplan.domain.tasktype.repository;

import capstone2.team3.realplan.domain.tasktype.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {

    Optional<TaskType> findByCode(String code);
}