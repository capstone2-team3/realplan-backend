package capstone2.team3.realplan.domain.tasktype.entity;

import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TaskType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_type_id")
    private Long taskTypeId;

    @Column(nullable = false, unique = true, length = 30)
    private String code;  // TIME_BASED, QUANTITY_BASED, SATISFACTION_BASED

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}