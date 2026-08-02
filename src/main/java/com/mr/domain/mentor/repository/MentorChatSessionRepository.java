package com.mr.domain.mentor.repository;

import com.mr.domain.mentor.entity.MentorChatSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentorChatSessionRepository extends JpaRepository<MentorChatSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from MentorChatSession session where session.analysis.id = :analysisId")
    Optional<MentorChatSession> findByAnalysisIdForUpdate(@Param("analysisId") Long analysisId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from MentorChatSession session where session.id = :sessionId")
    Optional<MentorChatSession> findByIdForUpdate(@Param("sessionId") Long sessionId);

    @Modifying(clearAutomatically = true)
    @Query("delete from MentorChatSession mcs where mcs.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
