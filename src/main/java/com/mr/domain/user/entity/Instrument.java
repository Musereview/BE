package com.mr.domain.user.entity;

import com.mr.domain.user.entity.enums.InstrumentErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "instrument")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instrument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder(access = AccessLevel.PRIVATE)
    private Instrument(String code, String name, boolean active) {
        validateCode(code);
        validateName(name);
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public static Instrument create(String code, String name) {
        return Instrument.builder()
                .code(code)
                .name(name)
                .active(true)
                .build();
    }

    private static void validateCode(String code) {
        if (code == null) {
            throw new GeneralException(InstrumentErrorStatus.CODE_REQUIRED);
        }
    }

    private static void validateName(String name) {
        if (name == null) {
            throw new GeneralException(InstrumentErrorStatus.NAME_REQUIRED);
        }
    }
}
