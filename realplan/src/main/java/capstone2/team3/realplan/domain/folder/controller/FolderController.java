package capstone2.team3.realplan.domain.folder.controller;

import capstone2.team3.realplan.domain.folder.dto.FolderCreateRequest;
import capstone2.team3.realplan.domain.folder.dto.FolderResponse;
import capstone2.team3.realplan.domain.folder.dto.FolderUpdateRequest;
import capstone2.team3.realplan.domain.folder.service.FolderService;
import capstone2.team3.realplan.global.common.ApiResponse;
import capstone2.team3.realplan.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Folder", description = "폴더 API")
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @Operation(summary = "폴더 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getFolders(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(folderService.getFolders(authUser.getUserId())));
    }

    @Operation(summary = "폴더 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody FolderCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(folderService.createFolder(authUser.getUserId(), request)));
    }

    @Operation(summary = "폴더명 수정")
    @PatchMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> updateFolder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long folderId,
            @Valid @RequestBody FolderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(folderService.updateFolder(authUser.getUserId(), folderId, request)));
    }

    @Operation(summary = "폴더 삭제 (태스크는 기본 폴더로 이동)")
    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long folderId) {
        folderService.deleteFolder(authUser.getUserId(), folderId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
