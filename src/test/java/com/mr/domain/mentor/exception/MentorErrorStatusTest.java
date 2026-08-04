package com.mr.domain.mentor.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MentorErrorStatusTest {

    @Test
    void errorCodesAreUnique() {
        assertThat(Arrays.stream(MentorErrorStatus.values())
                .map(MentorErrorStatus::getCode))
                .doesNotHaveDuplicates();
    }
}
