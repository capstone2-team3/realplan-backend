package capstone2.team3.realplan.domain.dailyplan.repository;

import capstone2.team3.realplan.domain.dailyplan.entity.DailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

    Optional<DailyPlan> findByUserUserIdAndPlanDate(Long userId, LocalDate planDate);

    Optional<DailyPlan> findByDailyPlanIdAndUserUserId(Long dailyPlanId, Long userId);

    boolean existsByUserUserIdAndPlanDate(Long userId, LocalDate planDate);
}