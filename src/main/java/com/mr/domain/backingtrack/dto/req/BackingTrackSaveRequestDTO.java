package com.mr.domain.backingtrack.dto.req;

import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BackingTrackSaveRequestDTO {

    public record SaveDTO(
            @NotBlank(message = "백킹트랙 제목은 필수입니다.")
            @Size(max = 50, message = "백킹트랙 제목은 50자 이내여야 합니다.")
            String title,

            @NotBlank(message = "장르는 필수입니다.")
            @Size(max = 50, message = "장르는 50자 이내여야 합니다.")
            String genre,

            @NotBlank(message = "Key 정보는 필수입니다.")
            @Size(max = 20, message = "Key 정보는 20자 이내여야 합니다.")
            String keySignature,

            @NotNull(message = "조성은 필수입니다.")
            ScaleType scaleType,

            @NotBlank(message = "박자는 필수입니다.")
            @Pattern(regexp = "^([1-9]|1[0-6])/(2|4|8|16)$", message = "음악적으로 유효한 박자 형식이어야 합니다. (예: 4/4, 3/4, 6/8)")
            String timeSignature,

            @NotNull(message = "BPM은 필수입니다.")
            @Min(value = 50, message = "BPM은 50 이상이어야 합니다.")
            @Max(value = 200, message = "BPM은 200 이하여야 합니다.")
            Integer bpm,

            @NotNull(message = "재생 시간은 필수입니다.")
            @Min(value = 60, message = "재생 시간은 최소 60초(1분) 이상이어야 합니다.")
            @Max(value = 600, message = "재생 시간은 최대 600초(10분) 이하여야 합니다.")
            Integer playtimeSec,

            @Size(max = 255, message = "오디오 파일 URL은 255자 이내여야 합니다.")
            String audioFileUrl,

            @NotNull(message = "공개 범위는 필수입니다.")
            AccessLevel accessLevel,

            @NotNull(message = "난이도는 필수입니다.")
            Level level,

            @Valid
            @NotEmpty(message = "코드 진행 정보는 필수입니다.")
            List<ChordProgressionDTO> chordProgression
    ) {}

    public record ChordProgressionDTO(
            @NotNull(message = "마디 번호는 필수입니다.")
            @Min(value = 1, message = "마디 번호는 1 이상이어야 합니다.")
            Integer measureNo,

            @NotNull(message = "코드 순서는 필수입니다.")
            @Min(value = 1, message = "코드 순서는 1 이상이어야 합니다.")
            Integer sequenceNo,

            @NotBlank(message = "코드명은 필수입니다.")
            @Size(max = 30, message = "코드명은 30자 이내여야 합니다.")
            String chordName
    ) {}
}
