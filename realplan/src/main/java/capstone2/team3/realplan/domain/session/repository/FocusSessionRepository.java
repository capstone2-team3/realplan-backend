package capstone2.team3.realplan.domain.session.repository;

import capstone2.team3.realplan.domain.session.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    List<FocusSession> findAllByTaskTaskIdOrderByStartedAtDesc(Long taskId);

    Optional<FocusSession> findBySessionIdAndUserUserId(Long sessionId, Long userId);

    // 현재 진행 중인 세션 조회 (ACTIVE 또는 PAUSED)
    Optional<FocusSession> findByTaskTaskIdAndSessionStatusIn(
            Long taskId, List<FocusSession.SessionStatus> statuses);

    List<FocusSession> findAllByUserUserIdOrderByStartedAtDesc(Long userId);

    List<FocusSession> findAllByUserUserIdAndSessionStatusIn(
            Long userId, List<FocusSession.SessionStatus> statuses);

    List<FocusSession> findAllByUserUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndSessionStatus(
            Long userId,
            LocalDateTime start,
            LocalDateTime end,
            FocusSession.SessionStatus sessionStatus);
}
