package com.mr.domain.user.config;

import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InstrumentSeeder implements ApplicationRunner {

    private static final String PIANO_CODE = "PIANO";
    private static final String PIANO_NAME = "피아노";

    private final InstrumentRepository instrumentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        instrumentRepository.findByCode(PIANO_CODE)
                .orElseGet(() -> instrumentRepository.save(Instrument.create(PIANO_CODE, PIANO_NAME)));
    }
}
