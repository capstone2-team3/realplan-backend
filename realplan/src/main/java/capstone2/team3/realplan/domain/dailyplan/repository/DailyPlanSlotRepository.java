package capstone2.team3.realplan.domain.dailyplan.repository;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlanSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DailyPlanSlotRepository extends JpaRepository<DailyPlanSlot, Long> {

    List<DailyPlanSlot> findAllByDailyPlanDailyPlanIdOrderBySlotIndexAsc(Long dailyPlanId);

    Optional<DailyPlanSlot> findBySlotIdAndDailyPlanDailyPlanId(Long slotId, Long dailyPlanId);

    // AI 배치 결과 적용 시 슬롯 인덱스로 조회
    Optional<DailyPlanSlot> findByDailyPlanDailyPlanIdAndSlotIndex(Long dailyPlanId, int slotIndex);

    @Modifying
    @Query("DELETE FROM DailyPlanSlot s WHERE s.dailyPlan.dailyPlanId = :dailyPlanId")
    void deleteAllByDailyPlanDailyPlanId(@Param("dailyPlanId") Long dailyPlanId);
}