package com.mr.domain.mentor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.repository.MentorMessageRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorService {

    private final AnalysisRepository analysisRepository;
    private final MentorMessageRepository mentorMessageRepository;
    private final ObjectMapper objectMapper;

    public MentorMessageHistoryResponseDTO getMessageHistory(Long userId, Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
        validateOwner(analysis, userId);

        List<MentorMessageHistoryResponseDTO.Message> messages = mentorMessageRepository
                .findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(analysisId)
                .stream()
                .map(message -> MentorMessageHistoryResponseDTO.Message.from(
                        message,
                        parseReferences(message.getReferencesJson())
                ))
                .toList();

        return new MentorMessageHistoryResponseDTO(analysisId, messages);
    }

    private void validateOwner(Analysis analysis, Long userId) {
        if (!Objects.equals(analysis.getUser().getUserId(), userId)) {
            throw new GeneralException(MentorErrorStatus.MENTOR_ACCESS_DENIED);
        }
    }

    private JsonNode parseReferences(String referencesJson) {
        if (referencesJson == null || referencesJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(referencesJson);
        } catch (JsonProcessingException exception) {
            throw new GeneralException(MentorErrorStatus.MENTOR_HISTORY_RETRIEVAL_FAILED);
        }
    }
}
