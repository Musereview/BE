package com.mr.domain.backingTrack.service;

import com.mr.domain.backingTrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.backingTrack.entity.ChordProgression;
import com.mr.domain.backingTrack.exception.BackingTrackErrorStatus;
import com.mr.domain.backingTrack.repository.BackingTrackRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackingTrackService {

    private final BackingTrackRepository backingTrackRepository;
    private final UserRepository userRepository;

    private static final Long TEMP_DEFAULT_ACADEMY_ID = 1L; // MVP 임시 학원 ID

    // 백킹트랙 생성
    @Transactional
    public BackingTrackCreateResponseDTO.CreateResultDTO createBackingTrack(
            Long userId,
            BackingTrackSaveRequestDTO.SaveDTO request
    ){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        validateChordDuplicates(request.chordProgression());
        validateChordSequence(request.timeSignature(), request.chordProgression());

        BackingTrack backingTrack = BackingTrack.create(
                user,
                TEMP_DEFAULT_ACADEMY_ID,
                request.title(),
                request.genre(),
                request.keySignature(),
                request.scaleType(),
                request.timeSignature(),
                request.bpm(),
                request.playtimeSec(),
                request.audioFileUrl(),
                null,   // midi 데이터는 생성 시 null로 초기화 (mvp후 별도 API나 이벤트로 업데이트)
                request.accessLevel(),
                request.level()
        );

        request.chordProgression().forEach(chordDTO -> {
            ChordProgression chord = ChordProgression.create(
                    backingTrack,
                    chordDTO.sequenceNo(),
                    chordDTO.measureNo(),
                    chordDTO.chordName()
            );
        });

        BackingTrack savedTrack = backingTrackRepository.save(backingTrack);

        return BackingTrackCreateResponseDTO.CreateResultDTO.of(
                savedTrack.getId(),
                savedTrack.getTitle(),
                savedTrack.getCreatedAt()
        );
    }

    // 백킹트랙 수정
    @Transactional
    public BackingTrackUpdateResponseDTO.UpdateResultDTO updateBackingTrack(
            Long userId,
            Long backingTrackId,
            BackingTrackSaveRequestDTO.SaveDTO request
    ) {
        validateChordDuplicates(request.chordProgression());
        validateChordSequence(request.timeSignature(), request.chordProgression());

        BackingTrack backingTrack = backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId)
                .orElseThrow(() -> new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

        // 수정 권한 검증 (작성자 본인 확인)
        if (!backingTrack.getUser().getUserId().equals(userId)) {
            throw new GeneralException(BackingTrackErrorStatus.FORBIDDEN_UPDATE);
        }

        // 엔티티 데이터 업데이트
        backingTrack.updateTrackInfo(
                request.title(),
                request.genre(),
                request.keySignature(),
                request.scaleType(),
                request.timeSignature(),
                request.bpm(),
                request.playtimeSec(),
                request.audioFileUrl(),
                request.accessLevel(),
                request.level()
        );

        // 기존 코드 진행 비우고 새로운 리스트로 교체
        backingTrack.getChordProgressions().clear();

        request.chordProgression().forEach(chordDTO -> {
            ChordProgression chord = ChordProgression.create(
                    backingTrack,
                    chordDTO.sequenceNo(),
                    chordDTO.measureNo(),
                    chordDTO.chordName()
            );
        });

        return BackingTrackUpdateResponseDTO.UpdateResultDTO.of(
                backingTrack.getId(),
                backingTrack.getTitle(),
                backingTrack.getUpdatedAt()
        );
    }

    private void validateChordDuplicates(List<BackingTrackSaveRequestDTO.ChordProgressionDTO> chordProgressions) {
        boolean hasDuplicates = chordProgressions.stream()
                .map(chord -> chord.measureNo() + "-" + chord.sequenceNo())
                .distinct()
                .count() != chordProgressions.size();

        if (hasDuplicates) {
            throw new GeneralException(BackingTrackErrorStatus.DUPLICATE_CHORD_POSITION);
        }
    }

    private void validateChordSequence(String timeSignature, List<BackingTrackSaveRequestDTO.ChordProgressionDTO> chordProgressions) {
        // 4/4 박이라면 분자인 4를 추출
        int maxSequencePerMeasure = Integer.parseInt(timeSignature.split("/")[0]);

        for (BackingTrackSaveRequestDTO.ChordProgressionDTO chord : chordProgressions) {
            // 만약 4/4박자인데 sequenceNo가 5 이상으로 들어오면 예외 발생
            if (chord.sequenceNo() > maxSequencePerMeasure) {
                throw new GeneralException(BackingTrackErrorStatus.INVALID_CHORD_SEQUENCE);
            }
        }
    }
}
