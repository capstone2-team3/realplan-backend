package capstone2.team3.realplan.domain.dailyplan.repository;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyPlanSessionRepository extends JpaRepository<DailyPlanSession, Long> {

    // 플랜 태스크 내 세션 목록 (순서대로)
    List<DailyPlanSession> findAllByDailyPlanTaskDailyPlanTaskIdOrderBySessionOrderAsc(Long dailyPlanTaskId);

    // 플랜 태스크 내 세션 전체 삭제 (재분할 시 사용)
    void deleteAllByDailyPlanTaskDailyPlanTaskId(Long dailyPlanTaskId);
}