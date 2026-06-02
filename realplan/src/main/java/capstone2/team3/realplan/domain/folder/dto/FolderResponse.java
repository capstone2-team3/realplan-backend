package capstone2.team3.realplan.domain.folder.dto;

import capstone2.team3.realplan.domain.folder.entity.Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "폴더 응답")
public class FolderResponse {

    @Schema(example = "2")
    private final Long folderId;

    @Schema(example = "학교 과제")
    private final String name;

    @Schema(example = "false")
    private final boolean isDefault;

    @Schema(example = "2026-06-03T22:30:00")
    private final LocalDateTime createdAt;

    @Schema(example = "2026-06-03T22:30:00")
    private final LocalDateTime updatedAt;

    private FolderResponse(Folder folder) {
        this.folderId = folder.getFolderId();
        this.name = folder.getName();
        this.isDefault = folder.isDefault();
        this.createdAt = folder.getCreatedAt();
        this.updatedAt = folder.getUpdatedAt();
    }

    public static FolderResponse from(Folder folder) {
        return new FolderResponse(folder);
    }
}
