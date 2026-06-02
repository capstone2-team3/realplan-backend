package capstone2.team3.realplan.domain.folder.controller;

import capstone2.team3.realplan.domain.folder.dto.FolderCreateRequest;
import capstone2.team3.realplan.domain.folder.dto.FolderResponse;
import capstone2.team3.realplan.domain.folder.dto.FolderUpdateRequest;
import capstone2.team3.realplan.domain.folder.service.FolderService;
import capstone2.team3.realplan.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Folder", description = "폴더 API")
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    // TODO: JWT 구현 후 @AuthenticationPrincipal로 userId 추출 예정
    // 현재는 임시로 Header에서 userId를 받는 방식으로 테스트
    private static final String USER_ID_HEADER = "X-User-Id";

    @Operation(summary = "폴더 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getFolders(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(folderService.getFolders(userId)));
    }

    @Operation(summary = "폴더 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody FolderCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(folderService.createFolder(userId, request)));
    }

    @Operation(summary = "폴더명 수정")
    @PatchMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> updateFolder(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long folderId,
            @Valid @RequestBody FolderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(folderService.updateFolder(userId, folderId, request)));
    }

    @Operation(summary = "폴더 삭제 (태스크는 기본 폴더로 이동)")
    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long folderId) {
        folderService.deleteFolder(userId, folderId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
