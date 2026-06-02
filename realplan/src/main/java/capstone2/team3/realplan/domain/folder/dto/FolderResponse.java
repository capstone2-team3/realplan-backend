package capstone2.team3.realplan.domain.folder.dto;

import capstone2.team3.realplan.domain.folder.entity.Folder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FolderResponse {

    private final Long folderId;
    private final String name;
    private final boolean isDefault;
    private final LocalDateTime createdAt;
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