package capstone2.team3.realplan.domain.task.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TaskReminderReadRequest {

    @NotEmpty
    private List<Long> taskIds;
}
