package com.mr.domain.backingTrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.backingTrack.dto.req.BackingTrackCreateRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.backingTrack.entity.ChordProgression;
import com.mr.domain.backingTrack.repository.BackingTrackRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackingTrackService {

    private final BackingTrackRepository backingTrackRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BackingTrackCreateResponseDTO.CreateResultDTO createBackingTrack(
            Long userId,
            BackingTrackCreateRequestDTO.CreateDTO request
    ){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Long defaultAcademyId = 1L; // MVP 기준 관리자 학원 ID 기본값 세팅

        JsonNode midiNode = request.midiData() != null ? objectMapper.valueToTree(request.midiData()) : null;

        BackingTrack backingTrack = BackingTrack.create(
                user,
                defaultAcademyId,
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

        return BackingTrackCreateResponseDTO.CreateResultDTO.of(
                savedTrack.getId(),
                savedTrack.getTitle(),
                savedTrack.getCreatedAt()
        );
    }
}
