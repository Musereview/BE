package com.mr.domain.user.repository;

import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.StudentInstrument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentInstrumentRepository extends JpaRepository<StudentInstrument, Long> {

    Optional<StudentInstrument> findFirstByStudentAndPrimaryTrue(Student student);

    @Modifying
    @Query("delete from StudentInstrument si where si.student.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
