package capstone2.team3.realplan.domain.task.repository;

import capstone2.team3.realplan.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByFolderFolderId(Long folderId);

    List<Task> findAllByUserUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Task> findByTaskIdAndUserUserId(Long taskId, Long userId);

    long countByUserUserIdAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            Long userId, LocalDateTime start, LocalDateTime end);
}
