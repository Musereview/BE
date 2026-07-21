package com.mr.domain.user.entity;

import com.mr.domain.user.entity.enums.StudentInstrumentErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "student_instrument",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_instrument_student_id_instrument_id",
                        columnNames = {"student_id", "instrument_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentInstrument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_instrument_id")
    private Long studentInstrumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Builder(access = AccessLevel.PRIVATE)
    private StudentInstrument(Student student, Instrument instrument, boolean primary) {
        validateStudent(student);
        validateInstrument(instrument);
        this.student = student;
        this.instrument = instrument;
        this.primary = primary;
    }

    public static StudentInstrument createPrimary(Student student, Instrument instrument) {
        return StudentInstrument.builder()
                .student(student)
                .instrument(instrument)
                .primary(true)
                .build();
    }

    public static StudentInstrument createSecondary(Student student, Instrument instrument) {
        return StudentInstrument.builder()
                .student(student)
                .instrument(instrument)
                .primary(false)
                .build();
    }

    public void markAsPrimary() {
        this.primary = true;
    }

    public void unmarkAsPrimary() {
        this.primary = false;
    }

    private static void validateStudent(Student student) {
        if (student == null) {
            throw new GeneralException(StudentInstrumentErrorStatus.STUDENT_REQUIRED);
        }
    }

    private static void validateInstrument(Instrument instrument) {
        if (instrument == null) {
            throw new GeneralException(StudentInstrumentErrorStatus.INSTRUMENT_REQUIRED);
        }
    }
}
