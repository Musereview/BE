package com.mr.domain.mentor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.dto.res.MentorStreamEventDTO;
import com.mr.domain.mentor.entity.MentorChatSession;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.repository.MentorChatSessionRepository;
import com.mr.domain.mentor.repository.MentorMessageRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MentorQuestionService {

    private static final Duration STALE_GENERATION_AFTER = Duration.ofMinutes(2);
    private static final String REFERENCES_JSON =
            "{\"sourceFields\":[\"analysis.raw_result_json\",\"analysis_report.content\"]}";

    private final AnalysisRepository analysisRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final MentorChatSessionRepository sessionRepository;
    private final MentorMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PreparedQuestion prepare(Long userId, Long analysisId, String content) {
        validateContent(content);

        Analysis analysis = analysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
        validateOwner(analysis, userId);
        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new GeneralException(MentorErrorStatus.MENTOR_ANALYSIS_NOT_COMPLETED);
        }

        MentorChatSession session = sessionRepository.findByAnalysisIdForUpdate(analysisId)
                .orElseGet(() -> sessionRepository.save(
                        MentorChatSession.createActive(analysis, analysis.getUser())));
        String generationToken = session.startGenerating(STALE_GENERATION_AFTER);

        String prompt = buildPrompt(analysis, content.trim());
        MentorMessage userMessage = messageRepository.saveAndFlush(
                MentorMessage.createUserMessage(session, content.trim()));
        return new PreparedQuestion(
                session.getId(),
                generationToken,
                prompt,
                new MentorStreamEventDTO.Start(
                        analysisId,
                        session.getId(),
                        MentorStreamEventDTO.Message.user(userMessage)
                )
        );
    }

    @Transactional
    public MentorStreamEventDTO.Complete complete(Long sessionId, String generationToken, String answer) {
        MentorChatSession session = findSessionForUpdate(sessionId);
        session.completeGenerating(generationToken);
        MentorMessage assistantMessage = messageRepository.saveAndFlush(
                MentorMessage.createAssistantMessage(session, REFERENCES_JSON, answer));
        try {
            return new MentorStreamEventDTO.Complete(MentorStreamEventDTO.Message.assistant(
                    assistantMessage,
                    objectMapper.readTree(REFERENCES_JSON)
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid server-managed mentor references.", exception);
        }
    }

    @Transactional
    public void fail(Long sessionId, String generationToken) {
        findSessionForUpdate(sessionId).failGenerating(generationToken);
    }

    private MentorChatSession findSessionForUpdate(Long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new GeneralException(MentorErrorStatus.MENTOR_INVALID_REQUEST));
    }

    private String buildPrompt(Analysis analysis, String question) {
        AnalysisReport report = analysisReportRepository
                .findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(analysis.getId(), LlmStatus.SUCCESS)
                .orElse(null);
        List<MentorMessage> recentMessages = new ArrayList<>(
                messageRepository.findTop10ByMentorChatSessionAnalysisIdOrderByCreatedAtDescIdDesc(
                        analysis.getId()));
        Collections.reverse(recentMessages);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("analysis", analysis.getRawResultJson());
        context.put("report", report == null ? null : report.getContent());
        context.put("history", recentMessages.stream()
                .map(message -> Map.of(
                        "role", message.getRole().name(),
                        "content", message.getContent()
                ))
                .toList());
        context.put("question", question);
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new GeneralException(MentorErrorStatus.MENTOR_INVALID_REQUEST);
        }
    }

    private void validateOwner(Analysis analysis, Long userId) {
        if (!Objects.equals(analysis.getUser().getUserId(), userId)) {
            throw new GeneralException(MentorErrorStatus.MENTOR_ACCESS_DENIED);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GeneralException(MentorErrorStatus.MENTOR_QUESTION_REQUIRED);
        }
        if (content.trim().length() > 500) {
            throw new GeneralException(MentorErrorStatus.MENTOR_QUESTION_TOO_LONG);
        }
    }

    public record PreparedQuestion(
            Long sessionId,
            String generationToken,
            String prompt,
            MentorStreamEventDTO.Start startEvent
    ) {
    }
}
