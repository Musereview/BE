package com.mr.domain.backingTrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.backingTrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackSaveResponseDTO;
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
    private final ObjectMapper objectMapper;

    private static final Long TEMP_DEFAULT_ACADEMY_ID = 1L; // MVP 임시 학원 ID

    // 백킹트랙 생성
    @Transactional
    public BackingTrackSaveResponseDTO.SaveResultDTO createBackingTrack(
            Long userId,
            BackingTrackSaveRequestDTO.SaveDTO request
    ){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        validateChordDuplicates(request.chordProgression());


        JsonNode midiNode = request.midiData() != null ? objectMapper.valueToTree(request.midiData()) : null;

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
                midiNode,
                request.accessLevel(),
                request.level()
        );

        request.chordProgression().forEach(chordDTO ->
                ChordProgression.create(
                        backingTrack,
                        chordDTO.sequenceNo(),
                        chordDTO.measureNo(),
                        chordDTO.chordName()
                )
        );

        BackingTrack savedTrack = backingTrackRepository.save(backingTrack);

        return BackingTrackSaveResponseDTO.SaveResultDTO.of(
                savedTrack.getId(),
                savedTrack.getTitle(),
                savedTrack.getCreatedAt()
        );
    }

    // 백킹트랙 수정
    @Transactional
    public BackingTrackSaveResponseDTO.SaveResultDTO updateBackingTrack(
            Long userId,
            Long backingTrackId,
            BackingTrackSaveRequestDTO.SaveDTO request
    ) {
        validateChordDuplicates(request.chordProgression());

        BackingTrack backingTrack = backingTrackRepository.findById(backingTrackId)
                .orElseThrow(() -> new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

        // 수정 권한 검증 (작성자 본인 확인)
        if (!backingTrack.getUser().getUserId().equals(userId)) {
            throw new GeneralException(BackingTrackErrorStatus.FORBIDDEN_UPDATE);
        }

        JsonNode midiNode = request.midiData() != null ? objectMapper.valueToTree(request.midiData()) : null;

        // 엔티티 데이터 업데이트
        backingTrack.updateTrackInfo(
                request.title(),
                request.genre(),
                request.keySignature(),
                request.scaleType(),
                request.timeSignature(),
                request.bpm(),
                request.playtimeSec(),
                request.accessLevel(),
                request.level()
        );

        // 기존 코드 진행 비우고 새로운 리스트로 교체
        backingTrack.getChordProgressions().clear();
        request.chordProgression().forEach(chordDTO ->
                ChordProgression.create(
                        backingTrack,
                        chordDTO.sequenceNo(),
                        chordDTO.measureNo(),
                        chordDTO.chordName()
                )
        );

        return BackingTrackSaveResponseDTO.SaveResultDTO.of(
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
}
