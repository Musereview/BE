package com.mr.domain.mentor.repository;

import com.mr.domain.mentor.entity.MentorMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentorMessageRepository extends JpaRepository<MentorMessage, Long> {

    List<MentorMessage> findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(Long analysisId);

    @Modifying
    @Query("delete from MentorMessage mm where mm.mentorChatSession.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
