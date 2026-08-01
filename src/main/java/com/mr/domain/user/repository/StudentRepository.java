package com.mr.domain.user.repository;

import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser(User user);

    @Modifying
    @Query("delete from Student s where s.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
