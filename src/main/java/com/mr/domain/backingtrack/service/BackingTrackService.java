package com.mr.domain.backingtrack.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.backingtrack.dto.req.BackingTrackListRequestDTO;
import com.mr.domain.backingtrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingtrack.dto.req.BackingTrackUploadUrlRequest;
import com.mr.domain.backingtrack.dto.req.PlayCountIncreaseRequestDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackDetailResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackListResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackRecommendedResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackUploadUrlResponse;
import com.mr.domain.backingtrack.dto.res.PlayCountIncreaseResponseDTO;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.ChordProgression;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.exception.BackingTrackErrorStatus;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackingTrackService {

    private final BackingTrackRepository backingTrackRepository;
    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final S3FileService s3FileService;

    private static final Long TEMP_DEFAULT_ACADEMY_ID = 1L; // MVP 임시 학원 ID
    private static final int PAGE_SIZE = 9;

    // 백킹트랙 생성
    @Transactional
    public BackingTrackCreateResponseDTO.CreateResultDTO createBackingTrack(
            Long userId,
            BackingTrackSaveRequestDTO.SaveDTO request
    ){
        validateChordDuplicates(request.chordProgression());
        validateChordSequence(request.timeSignature(), request.chordProgression());

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        String audioObjectKey =
                request.audioObjectKey() == null || request.audioObjectKey().isBlank()
                        ? null
                        : request.audioObjectKey();

        if (audioObjectKey != null) {
            s3FileService.validateUploadedFile(userId, S3FileType.BACKING_TRACK, audioObjectKey);
        }

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
                audioObjectKey,
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
            backingTrack.addChordProgression(chord);
        });

        BackingTrack savedTrack = backingTrackRepository.save(backingTrack);

        return BackingTrackCreateResponseDTO.CreateResultDTO.of(
                savedTrack.getId(),
                savedTrack.getTitle(),
                savedTrack.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public BackingTrackUploadUrlResponse createAudioUploadUrl(
            Long userId, BackingTrackUploadUrlRequest request
    ) {
        return BackingTrackUploadUrlResponse.from(s3FileService.createPresignedUpload(
                userId,
                S3FileType.BACKING_TRACK,
                request.toCommand()
        ));
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
        backingTrack.validateOwner(userId);

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

        backingTrackRepository.flush(); // 완전히 비우도록

        request.chordProgression().forEach(chordDTO -> {
            ChordProgression chord = ChordProgression.create(
                    backingTrack,
                    chordDTO.sequenceNo(),
                    chordDTO.measureNo(),
                    chordDTO.chordName()
            );
            backingTrack.addChordProgression(chord);
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

    // 재생 수 증가
    @Transactional
    public PlayCountIncreaseResponseDTO.IncreaseResponseDTO increasePlayCount(
            Long backingTrackId,
            PlayCountIncreaseRequestDTO.IncreaseRequestDTO request,
            Long userId
    ) {

        // 백킹트랙 존재 여부 확인
        BackingTrack backingTrack = backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId)
                .orElseThrow(() -> new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

        // AI 분석 결과 조회
        Analysis analysis = analysisRepository.findById(request.analysisId())
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));

        // AI 분석 상태 검증
        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_COMPLETED);
        }

        // 본인 분석 결과인지
        analysis.validateOwner(userId);

        if (!analysis.getPlaying().getBackingTrack().getId().equals(backingTrackId)) {
            throw new GeneralException(BackingTrackErrorStatus.ANALYSIS_TRACK_MISMATCH);
        }

        // 재생 수 증가
        int updated = backingTrackRepository.increasePlayCount(backingTrackId);
        if (updated == 0) {
            throw new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND);
        }

        // 최신 값 다시 조회
        BackingTrack refreshed = backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId)
                .orElseThrow(() -> new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

        return PlayCountIncreaseResponseDTO.IncreaseResponseDTO.of(
                refreshed.getId(),
                refreshed.getPlayCount()
        );
    }

    // 백킹트랙 목록 조회
    public BackingTrackListResponseDTO.ListResponseDTO getBackingTracks(
            BackingTrackListRequestDTO.ListRequestDTO request,
            Long userId
    ) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);

        List<BackingTrack> tracks = backingTrackRepository.findVisibleTracksAfterCursor(
                AccessLevel.PUBLIC, userId, request.cursor(), pageable
        );

        boolean hasNext = tracks.size() > PAGE_SIZE;
        List<BackingTrack> pageTracks = hasNext ? tracks.subList(0, PAGE_SIZE) : tracks;

        List<BackingTrackListResponseDTO.TrackInfo> trackInfos = pageTracks.stream()
                .map(track -> {
                    List<String> chordNames = track.getChordProgressions().stream()
                            .sorted(Comparator.comparing(ChordProgression::getMeasureNo)
                                    .thenComparing(ChordProgression::getSequenceNo))
                            .map(ChordProgression::getChordName)
                            .toList();

                    return BackingTrackListResponseDTO.TrackInfo.of(
                            track.getId(),
                            track.getTitle(),
                            track.getGenre(),
                            track.getKeySignature(),
                            track.getScaleType().name(),
                            track.getTimeSignature(),
                            chordNames,
                            track.getBpm(),
                            track.getLevel().name(),
                            track.getPlaytimeSec()
                    );
                }).toList();
        Long nextCursor = pageTracks.isEmpty() ? null : pageTracks.get(pageTracks.size()-1).getId();

        return BackingTrackListResponseDTO.ListResponseDTO.of(trackInfos, nextCursor, hasNext);
    }

    // 백킹트랙 상세 조회
    public BackingTrackDetailResponseDTO.DetailResponseDTO getBackingTrackDetail(
            Long backingTrackId,
            Long userId
    ) {
        BackingTrack track = backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId)
                .orElseThrow(() -> new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

        if (track.getAccessLevel() == AccessLevel.PRIVATE && !track.getUser().getUserId().equals(userId)) {
            throw new GeneralException(BackingTrackErrorStatus.FORBIDDEN_READ);
        }

        List<BackingTrackDetailResponseDTO.ChordDetail> chordDetails = track.getChordProgressions().stream()
                .sorted(Comparator.comparing(ChordProgression::getMeasureNo)
                        .thenComparing(ChordProgression::getSequenceNo))
                .map(c -> BackingTrackDetailResponseDTO.ChordDetail.of(
                        c.getMeasureNo(),
                        c.getSequenceNo(),
                        c.getChordName()
                ))
                .toList();

        String creatorName = track.getUser().getNickname();

        String audioFileUrl = null;
        if (track.getAudioObjectKey() != null && !track.getAudioObjectKey().isBlank()) {
            audioFileUrl = s3FileService.createPresignedDownload(
                    track.getUser().getUserId(),
                    S3FileType.BACKING_TRACK,
                    track.getAudioObjectKey()
            );
        }

        return BackingTrackDetailResponseDTO.DetailResponseDTO.of(
                track.getId(),
                track.getTitle(),
                track.getGenre(),
                track.getKeySignature(),
                track.getScaleType().name(),
                track.getTimeSignature(),
                track.getBpm(),
                track.getPlaytimeSec(),
                track.getLevel().name(),
                creatorName,
                audioFileUrl,
                chordDetails
        );
    }

    // 추천 백킹트랙 조회 로직
    public BackingTrackRecommendedResponseDTO.RecommendedResponseDTO getRecommendedTracks() {

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);   // 현재로부터 일주일 전

        List<BackingTrack> topTracks = backingTrackRepository.findTop3RecommendedTracks(oneWeekAgo);

        List<BackingTrackRecommendedResponseDTO.TrackInfo> trackInfos = topTracks.stream()
                .map(track -> {
                    // 코드 진행 정렬 및 추출
                    List<String> chordNames = track.getChordProgressions().stream()
                            .sorted(Comparator.comparing(ChordProgression::getMeasureNo)
                                    .thenComparing(ChordProgression::getSequenceNo))
                            .map(ChordProgression::getChordName)
                            .toList();

                    // DTO 정적 팩토리 메서드(of) 매핑
                    return BackingTrackRecommendedResponseDTO.TrackInfo.of(
                            track.getId(),
                            track.getTitle(),
                            track.getGenre(),
                            track.getKeySignature(),
                            track.getScaleType().name(),
                            track.getTimeSignature(),
                            chordNames,
                            track.getBpm(),
                            track.getLevel().name(),
                            track.getPlaytimeSec(),
                            track.getPlayCount()
                    );
                })
                .collect(Collectors.toList());

        return BackingTrackRecommendedResponseDTO.RecommendedResponseDTO.of(trackInfos);
    }
}
