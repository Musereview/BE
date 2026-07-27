package com.mr.domain.user.repository;

import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.StudentInstrument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInstrumentRepository extends JpaRepository<StudentInstrument, Long> {

    Optional<StudentInstrument> findByStudentAndPrimaryTrue(Student student);
}
