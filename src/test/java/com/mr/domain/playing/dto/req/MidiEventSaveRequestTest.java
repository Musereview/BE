package com.mr.domain.playing.dto.req;

import com.mr.domain.playing.entity.enums.MidiType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MidiEventSaveRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("유효한 MIDI 이벤트 목록은 검증을 통과한다")
    void validateValidRequest() {
        // given
        MidiEventSaveRequest.MidiEventRequest event =
                createValidEvent();

        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(event)
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .isEmpty();
    }

    @Test
    @DisplayName("MIDI 이벤트 목록에 null 요소가 있으면 검증에 실패한다")
    void validateNullEventInsideList() {
        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        java.util.Collections.singletonList(null)
                );

        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .isNotEmpty();
    }

    @Test
    @DisplayName("리스트 내부 이벤트의 sequence가 음수이면 검증에 실패한다")
    void validateSequenceInsideList() {
        // given
        MidiEventSaveRequest.MidiEventRequest invalidEvent =
                new MidiEventSaveRequest.MidiEventRequest(
                        -1,
                        MidiType.NOTE_ON,
                        60,
                        100,
                        100L
                );

        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(invalidEvent)
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath().toString()
                    ).contains("events[0].sequence");

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "MIDI 이벤트 순서 값은 0 이상이어야 합니다."
                            );
                });
    }

    @Test
    @DisplayName("pitch가 음수이면 검증에 실패한다")
    void validateNegativePitch() {
        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(
                                new MidiEventSaveRequest.MidiEventRequest(
                                        0,
                                        MidiType.NOTE_ON,
                                        -1,
                                        100,
                                        100L
                                )
                        )
                );

        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains("events[0].pitch");
    }

    @Test
    @DisplayName("리스트 내부 이벤트의 pitch가 127을 초과하면 검증에 실패한다")
    void validatePitchInsideList() {
        // given
        MidiEventSaveRequest.MidiEventRequest invalidEvent =
                new MidiEventSaveRequest.MidiEventRequest(
                        0,
                        MidiType.NOTE_ON,
                        128,
                        100,
                        100L
                );

        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(invalidEvent)
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath().toString()
                    ).contains("events[0].pitch");

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "MIDI 피치 값은 127 이하여야 합니다."
                            );
                });
    }

    @Test
    @DisplayName("리스트 내부 이벤트의 velocity가 음수이면 검증에 실패한다")
    void validateVelocityInsideList() {
        // given
        MidiEventSaveRequest.MidiEventRequest invalidEvent =
                new MidiEventSaveRequest.MidiEventRequest(
                        0,
                        MidiType.NOTE_ON,
                        60,
                        -1,
                        100L
                );

        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(invalidEvent)
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath().toString()
                    ).contains("events[0].velocity");

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "MIDI 입력 강도 값은 0 이상이어야 합니다."
                            );
                });
    }

    @Test
    @DisplayName("velocity가 127을 초과하면 검증에 실패한다")
    void validateExceededVelocity() {
        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(
                                new MidiEventSaveRequest.MidiEventRequest(
                                        0,
                                        MidiType.NOTE_ON,
                                        60,
                                        128,
                                        100L
                                )
                        )
                );

        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains("events[0].velocity");
    }

    @Test
    @DisplayName("리스트 내부 이벤트의 timestampMs가 음수이면 검증에 실패한다")
    void validateTimestampInsideList() {
        // given
        MidiEventSaveRequest.MidiEventRequest invalidEvent =
                new MidiEventSaveRequest.MidiEventRequest(
                        0,
                        MidiType.NOTE_ON,
                        60,
                        100,
                        -1L
                );

        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(invalidEvent)
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath().toString()
                    ).contains("events[0].timestampMs");

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "MIDI 이벤트 발생 시간은 0 이상이어야 합니다."
                            );
                });
    }

    @Test
    @DisplayName("리스트 내부 이벤트의 필수값이 null이면 검증에 실패한다")
    void validateNullFieldsInsideList() {
        // given
        MidiEventSaveRequest.MidiEventRequest invalidEvent =
                new MidiEventSaveRequest.MidiEventRequest(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of(invalidEvent)
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .extracting(
                        violation ->
                                violation.getPropertyPath().toString()
                )
                .anyMatch(path ->
                        path.contains("events[0].sequence")
                )
                .anyMatch(path ->
                        path.contains("events[0].type")
                )
                .anyMatch(path ->
                        path.contains("events[0].pitch")
                )
                .anyMatch(path ->
                        path.contains("events[0].velocity")
                )
                .anyMatch(path ->
                        path.contains("events[0].timestampMs")
                );
    }

    @Test
    @DisplayName("MIDI 이벤트 목록이 비어 있으면 검증에 실패한다")
    void validateEmptyEvents() {
        // given
        MidiEventSaveRequest request =
                new MidiEventSaveRequest(
                        List.of()
                );

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath().toString()
                    ).isEqualTo("events");

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "MIDI 이벤트 목록은 필수입니다"
                            );
                });
    }

    @Test
    @DisplayName("MIDI 이벤트 목록이 null이면 검증에 실패한다")
    void validateNullEvents() {
        // given
        MidiEventSaveRequest request =
                new MidiEventSaveRequest(null);

        // when
        Set<ConstraintViolation<MidiEventSaveRequest>> violations =
                validator.validate(request);

        // then
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath().toString()
                    ).isEqualTo("events");
                });
    }

    private MidiEventSaveRequest.MidiEventRequest createValidEvent() {
        return new MidiEventSaveRequest.MidiEventRequest(
                0,
                MidiType.NOTE_ON,
                60,
                100,
                100L
        );
    }
}