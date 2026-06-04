package capstone2.team3.realplan.domain.session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_feedback")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private FocusSession session;

    // 진행 속도 1~5
    @Column(name = "progress_level", nullable = false)
    private int progressLevel;

    // 세션 후 누적 진행률 %
    @Column(name = "progress_percent_after")
    private Integer progressPercentAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_level", nullable = false, length = 10)
    private FocusLevel focusLevel;

    @Column(name = "ai_remaining_before")
    private Integer aiRemainingBefore;

    @Column(name = "ai_remaining_after")
    private Integer aiRemainingAfter;

    @Column(name = "previous_ai_total_minutes")
    private Integer previousAiTotalMinutes;

    @Column(name = "updated_ai_total_minutes")
    private Integer updatedAiTotalMinutes;

    @Column(name = "progress_based_remaining_minutes")
    private Integer progressBasedRemainingMinutes;

    @Column(name = "normalized_remaining_minutes")
    private Integer normalizedRemainingMinutes;

    @Column(name = "blending_weight", precision = 10, scale = 6)
    private BigDecimal blendingWeight;

    @Column(name = "focus_weight", precision = 10, scale = 6)
    private BigDecimal focusWeight;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void applyAiEstimate(
            int previousAiTotalMinutes,
            int updatedAiTotalMinutes,
            int progressBasedRemainingMinutes,
            int normalizedRemainingMinutes,
            double blendingWeight,
            double focusWeight,
            int aiRemainingAfter
    ) {
        this.previousAiTotalMinutes = previousAiTotalMinutes;
        this.updatedAiTotalMinutes = updatedAiTotalMinutes;
        this.progressBasedRemainingMinutes = progressBasedRemainingMinutes;
        this.normalizedRemainingMinutes = normalizedRemainingMinutes;
        this.blendingWeight = toScaledDecimal(blendingWeight);
        this.focusWeight = toScaledDecimal(focusWeight);
        this.aiRemainingAfter = aiRemainingAfter;
    }

    private BigDecimal toScaledDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    // ── Enum ─────────────────────────────────────────

    public enum FocusLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}
