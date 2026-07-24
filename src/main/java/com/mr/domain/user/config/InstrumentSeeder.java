package com.mr.domain.user.config;

import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentSeeder implements ApplicationRunner {

    private static final String PIANO_CODE = "PIANO";
    private static final String PIANO_NAME = "피아노";

    private final InstrumentRepository instrumentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (instrumentRepository.findByCode(PIANO_CODE).isPresent()) {
            return;
        }

        try {
            instrumentRepository.save(Instrument.create(PIANO_CODE, PIANO_NAME));
        } catch (DataIntegrityViolationException e) {
            // 여러 인스턴스가 동시에 기동해 경쟁 상태로 중복 저장을 시도한 경우
            // code unique 제약으로 한쪽만 성공하므로, 나머지는 무시하고 넘어감
            log.info("PIANO 악기 시드 데이터가 다른 인스턴스에 의해 이미 생성됨");
        }
    }
}
