package capstone2.team3.realplan.domain.dailyplan.repository;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyPlanTaskRepository extends JpaRepository<DailyPlanTask, Long> {

    List<DailyPlanTask> findAllByDailyPlanDailyPlanIdOrderByDisplayOrderAsc(Long dailyPlanId);

    Optional<DailyPlanTask> findByDailyPlanTaskIdAndDailyPlanDailyPlanId(
            Long dailyPlanTaskId, Long dailyPlanId);

    Optional<DailyPlanTask> findByDailyPlanTaskIdAndDailyPlanUserUserIdAndTaskTaskId(
            Long dailyPlanTaskId, Long userId, Long taskId);

    // 플랜에 이미 해당 태스크가 있는지 확인 (중복 생성 방지)
    Optional<DailyPlanTask> findByDailyPlanDailyPlanIdAndTaskTaskId(
            Long dailyPlanId, Long taskId);
}
