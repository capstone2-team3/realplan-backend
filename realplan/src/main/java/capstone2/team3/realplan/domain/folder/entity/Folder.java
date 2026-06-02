package capstone2.team3.realplan.domain.folder.entity;

import capstone2.team3.realplan.domain.user.entity.User;
import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "folder",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_folder_user_name",
                columnNames = {"user_id", "name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Folder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    public void updateName(String name) {
        this.name = name;
    }
}