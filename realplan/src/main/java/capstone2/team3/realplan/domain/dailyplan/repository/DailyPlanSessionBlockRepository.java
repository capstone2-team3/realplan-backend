package capstone2.team3.realplan.domain.dailyplan.repository;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanSessionBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyPlanSessionBlockRepository extends JpaRepository<DailyPlanSessionBlock, Long> {

    // 논리 세션의 배치 블록 목록 (순서대로)
    List<DailyPlanSessionBlock> findAllByDailyPlanSessionDailyPlanSessionIdOrderByBlockOrderAsc(Long dailyPlanSessionId);

    // 논리 세션의 배치 블록 전체 삭제 (재배치 시 사용)
    void deleteAllByDailyPlanSessionDailyPlanSessionId(Long dailyPlanSessionId);
}