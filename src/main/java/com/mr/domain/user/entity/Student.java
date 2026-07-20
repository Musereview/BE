package com.mr.domain.user.entity;

import com.mr.domain.user.entity.enums.TheoryLevel;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "theory_level", nullable = false, length = 20)
    private TheoryLevel theoryLevel;

    @Builder(access = AccessLevel.PRIVATE)
    private Student(User user, TheoryLevel theoryLevel) {
        this.user = user;
        this.theoryLevel = theoryLevel;
    }

    public static Student create(User user, TheoryLevel theoryLevel) {
        return Student.builder()
                .user(user)
                .theoryLevel(theoryLevel)
                .build();
    }

    public void updateTheoryLevel(TheoryLevel theoryLevel) {
        this.theoryLevel = theoryLevel;
    }
}
