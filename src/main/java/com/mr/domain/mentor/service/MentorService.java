package com.mr.domain.mentor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.repository.MentorMessageRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorService {

    private final AnalysisRepository analysisRepository;
    private final MentorMessageRepository mentorMessageRepository;
    private final ObjectMapper objectMapper;

    public MentorMessageHistoryResponseDTO getMessageHistory(Long userId, Long analysisId) {
        Long ownerId = analysisRepository.findUserIdById(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
        validateOwner(ownerId, userId);

        List<MentorMessageHistoryResponseDTO.Message> messages = mentorMessageRepository
                .findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(analysisId)
                .stream()
                .map(message -> MentorMessageHistoryResponseDTO.Message.from(
                        message,
                        parseReferences(message.getId(), message.getReferencesJson())
                ))
                .toList();

        return new MentorMessageHistoryResponseDTO(analysisId, messages);
    }

    private void validateOwner(Long ownerId, Long userId) {
        if (!Objects.equals(ownerId, userId)) {
            throw new GeneralException(MentorErrorStatus.MENTOR_ACCESS_DENIED);
        }
    }

    private JsonNode parseReferences(Long mentorMessageId, String referencesJson) {
        if (referencesJson == null || referencesJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(referencesJson);
        } catch (JsonProcessingException exception) {
            log.warn("AI 멘토 판단 근거 JSON 파싱 실패. mentorMessageId={}", mentorMessageId);
            return null;
        }
    }
}
