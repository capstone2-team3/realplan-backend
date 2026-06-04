package capstone2.team3.realplan.domain.session.repository;

import capstone2.team3.realplan.domain.session.entity.SessionPauseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionPauseEventRepository extends JpaRepository<SessionPauseEvent, Long> {

    List<SessionPauseEvent> findAllBySessionSessionIdOrderByPausedAtAsc(Long sessionId);

    // 가장 최근 미재개 일시정지 이벤트 (resumed_at = null)
    Optional<SessionPauseEvent> findTopBySessionSessionIdAndResumedAtIsNullOrderByPausedAtDesc(
            Long sessionId);
}