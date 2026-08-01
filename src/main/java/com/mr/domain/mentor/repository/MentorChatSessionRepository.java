package com.mr.domain.mentor.repository;

import com.mr.domain.mentor.entity.MentorChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentorChatSessionRepository extends JpaRepository<MentorChatSession, Long> {

    @Modifying
    @Query("delete from MentorChatSession mcs where mcs.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
