package capstone2.team3.realplan.domain.session.repository;

import capstone2.team3.realplan.domain.session.entity.UserTaskTypeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTaskTypeProfileRepository extends JpaRepository<UserTaskTypeProfile, Long> {

    List<UserTaskTypeProfile> findAllByUserUserIdOrderByTaskTypeCodeAsc(Long userId);

    Optional<UserTaskTypeProfile> findByUserUserIdAndTaskTypeTaskTypeId(Long userId, Long taskTypeId);
}
