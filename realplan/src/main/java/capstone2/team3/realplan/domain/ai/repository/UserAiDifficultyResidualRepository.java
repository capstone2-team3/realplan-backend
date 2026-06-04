package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.UserAiDifficultyResidual;
import capstone2.team3.realplan.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAiDifficultyResidualRepository extends JpaRepository<UserAiDifficultyResidual, Long> {

    List<UserAiDifficultyResidual> findAllByUserUserId(Long userId);

    Optional<UserAiDifficultyResidual> findByUserUserIdAndDifficulty(Long userId, Task.Difficulty difficulty);
}
