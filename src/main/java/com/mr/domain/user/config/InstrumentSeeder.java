package com.mr.domain.user.config;

import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentSeeder implements ApplicationRunner {

    private static final String PIANO_CODE = "PIANO";
    private static final String PIANO_NAME = "피아노";
    private static final String POSTGRES_UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final InstrumentRepository instrumentRepository;

    // NOTE: run() 자체엔 @Transactional을 걸지 않음 — PostgreSQL은 트랜잭션 내 한 문장이라도
    // 제약 위반으로 실패하면 그 트랜잭션 전체가 abort 상태가 되어, 이후 catch로 예외를 소비해도
    // 커밋 시점에 다시 실패함. saveAndFlush() 각각이 Spring Data JPA의 자체 트랜잭션으로 독립 처리되도록
    // run() 레벨 트랜잭션을 없애서, 중복 저장 실패가 그 saveAndFlush() 호출 안에서 깔끔하게 끝나게 함.
    @Override
    public void run(ApplicationArguments args) {
        if (instrumentRepository.findByCode(PIANO_CODE).isPresent()) {
            return;
        }

        try {
            instrumentRepository.saveAndFlush(Instrument.create(PIANO_CODE, PIANO_NAME));
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateKeyViolation(e)) {
                throw e;
            }
            // 여러 인스턴스가 동시에 기동해 경쟁 상태로 중복 저장을 시도한 경우
            // code unique 제약으로 한쪽만 성공하므로, 나머지는 무시하고 넘어감
            log.info("PIANO 악기 시드 데이터가 다른 인스턴스에 의해 이미 생성됨");
        }
    }

    private boolean isDuplicateKeyViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException constraintViolationException
                && POSTGRES_UNIQUE_VIOLATION_SQL_STATE.equals(constraintViolationException.getSQLState());
    }
}
