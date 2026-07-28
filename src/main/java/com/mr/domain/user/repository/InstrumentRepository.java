package com.mr.domain.user.repository;

import com.mr.domain.user.entity.Instrument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByCode(String code);
}
