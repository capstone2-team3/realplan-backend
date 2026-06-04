package capstone2.team3.realplan.domain.session.repository;

import capstone2.team3.realplan.domain.session.entity.SessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    Optional<SessionFeedback> findBySessionSessionId(Long sessionId);

    List<SessionFeedback> findAllBySessionSessionIdIn(Collection<Long> sessionIds);
}
