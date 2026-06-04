package capstone2.team3.realplan.domain.dailyplan.entity;

import capstone2.team3.realplan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * DailyPlanSlot
 *
 * 역할: Home 화면의 WhenToMeet 스타일 가용시간 그리드를 저장
 *
 * slot_index 계산:
 *   index = (hour - 6) * 2 + (minute / 30)
 *   예) 09:00 → 6, 09:30 → 7, 13:00 → 14
 *   범위: 0~41 (06:00~27:00)
 *
 * daily_plan_task_id:
 *   NULL  = 가용시간이지만 태스크 미배정 (비어있는 슬롯)
 *   NOT NULL = 해당 태스크가 이 슬롯에 배정됨
 */
@Entity
@Table(
        name = "daily_plan_slot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_slot_plan_index",
                columnNames = {"daily_plan_id", "slot_index"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyPlanSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id", nullable = false)
    private DailyPlan dailyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 0~41 (06:00~27:00, 30분 단위)
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    // 이 슬롯에 배정된 플랜 태스크 (NULL = 미배정)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_task_id")
    private DailyPlanTask dailyPlanTask;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 비즈니스 메서드 ──────────────────────────────

    /** 슬롯에 태스크 배정 */
    public void assignTask(DailyPlanTask dailyPlanTask) {
        this.dailyPlanTask = dailyPlanTask;
    }

    /** 슬롯 태스크 배정 해제 */
    public void unassignTask() {
        this.dailyPlanTask = null;
    }

    /** 슬롯 인덱스 → 시간 문자열 변환 (예: 6 → "09:00") */
    public String toTimeLabel() {
        int totalMinutes = 6 * 60 + slotIndex * 30;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }
}