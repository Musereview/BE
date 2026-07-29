package com.mr.domain.user.repository;

import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser(User user);
}
