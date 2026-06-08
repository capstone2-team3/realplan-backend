package capstone2.team3.realplan.domain.folder.service;

import capstone2.team3.realplan.domain.folder.dto.FolderCreateRequest;
import capstone2.team3.realplan.domain.folder.dto.FolderResponse;
import capstone2.team3.realplan.domain.folder.dto.FolderUpdateRequest;
import capstone2.team3.realplan.domain.folder.entity.Folder;
import capstone2.team3.realplan.domain.folder.repository.FolderRepository;
import capstone2.team3.realplan.domain.task.repository.TaskRepository;
import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.domain.user.repository.UserRepository;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    // 폴더 목록 조회
    public List<FolderResponse> getFolders(Long userId) {
        return folderRepository.findAllByUserUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(FolderResponse::from)
                .toList();
    }

    // 폴더 생성
    @Transactional
    public FolderResponse createFolder(Long userId, FolderCreateRequest request) {
        // 이름 중복 검사
        if (folderRepository.existsByUserUserIdAndName(userId, request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_FOLDER_NAME);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Folder folder = Folder.builder()
                .user(user)
                .name(request.getName())
                .isDefault(false)
                .build();

        return FolderResponse.from(folderRepository.save(folder));
    }

    // 폴더명 수정
    @Transactional
    public FolderResponse updateFolder(Long userId, Long folderId, FolderUpdateRequest request) {
        Folder folder = getFolderOrThrow(userId, folderId);

        // 기본 폴더는 이름 변경 불가
        if (folder.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_FOLDER_CANNOT_BE_DELETED);
        }

        // 변경하려는 이름이 이미 존재하는지 확인 (자기 자신 제외)
        if (!folder.getName().equals(request.getName())
                && folderRepository.existsByUserUserIdAndName(userId, request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_FOLDER_NAME);
        }

        folder.updateName(request.getName());
        return FolderResponse.from(folder);
    }

    // 폴더 삭제
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        Folder folder = getFolderOrThrow(userId, folderId);

        // 기본 폴더 삭제 불가
        if (folder.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_FOLDER_CANNOT_BE_DELETED);
        }

        // 해당 폴더의 태스크들을 기본 폴더로 이동
        Folder defaultFolder = folderRepository.findByUserUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_FOLDER_NOT_FOUND));

        taskRepository.findAllByFolderFolderIdAndDeletedAtIsNull(folderId)
                .forEach(task -> task.updateFolder(defaultFolder));

        folderRepository.delete(folder);
    }

    // ── 내부 헬퍼 ────────────────────────────────────

    private Folder getFolderOrThrow(Long userId, Long folderId) {
        return folderRepository.findByFolderIdAndUserUserId(folderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
    }
}
