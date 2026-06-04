package capstone2.team3.realplan.domain.ai.repository;

import capstone2.team3.realplan.domain.ai.entity.UserAiFolderResidual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAiFolderResidualRepository extends JpaRepository<UserAiFolderResidual, Long> {

    List<UserAiFolderResidual> findAllByUserUserId(Long userId);

    Optional<UserAiFolderResidual> findByUserUserIdAndFolderFolderId(Long userId, Long folderId);
}
