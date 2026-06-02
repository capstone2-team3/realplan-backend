package capstone2.team3.realplan.domain.folder.repository;

import capstone2.team3.realplan.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByUserUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Folder> findByFolderIdAndUserUserId(Long folderId, Long userId);

    boolean existsByUserUserIdAndName(Long userId, String name);

    Optional<Folder> findByUserUserIdAndIsDefaultTrue(Long userId);
}