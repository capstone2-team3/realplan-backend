package capstone2.team3.realplan.domain.session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_pause_event")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SessionPauseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pause_event_id")
    private Long pauseEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private FocusSession session;

    @Column(name = "paused_at", nullable = false)
    private LocalDateTime pausedAt;

    // 재개 시각 — 미재개 시 NULL
    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void resume(LocalDateTime resumedAt) {
        this.resumedAt = resumedAt;
    }
}