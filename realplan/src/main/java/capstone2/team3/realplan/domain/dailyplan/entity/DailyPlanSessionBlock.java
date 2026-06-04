package capstone2.team3.realplan.domain.dailyplan.entity;

import capstone2.team3.realplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * DailyPlanSessionBlock
 *
 * 역할: Python AI /schedules/auto-place 결과를 저장하는 테이블
 *
 * DailyPlanSession(논리 세션)이 실제 시간표의 어느 슬롯에 배치됐는지
 * 30분 단위 조각(block)으로 기록.
 *
 * 예) 60분 논리 세션 → block_order=1(09:00~09:30), block_order=2(09:30~10:00)
 *
 * start_time/end_time을 VARCHAR로 저장하는 이유:
 * 27:00처럼 자정을 넘는 시간 표현을 허용하기 위해 문자열로 저장
 * (예: 25:30 = 다음날 01:30)
 */
@Entity
@Table(
        name = "daily_plan_session_block",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_block_session_order",
                columnNames = {"daily_plan_session_id", "block_order"}
        ),
        indexes = {
                @Index(name = "idx_dpsb_session", columnList = "daily_plan_session_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyPlanSessionBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_block_id")
    private Long sessionBlockId;

    // 소속 논리 세션 (삭제 시 함께 삭제)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_session_id", nullable = false)
    private DailyPlanSession dailyPlanSession;

    // 같은 논리 세션 내 배치 조각 순서 (1부터)
    @Column(name = "block_order", nullable = false)
    private int blockOrder;

    // 배치 시작 시각 (HH:MM, 27:00까지 허용)
    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    // 배치 종료 시각
    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime;

    // 배치 조각 길이 (분, 보통 30분 단위)
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;
}