package com.mr.domain.mentor.repository;

import com.mr.domain.mentor.entity.MentorMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorMessageRepository extends JpaRepository<MentorMessage, Long> {

    List<MentorMessage> findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(Long analysisId);
}
