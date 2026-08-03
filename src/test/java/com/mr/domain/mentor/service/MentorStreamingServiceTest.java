package com.mr.domain.mentor.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr.domain.mentor.dto.res.MentorStreamEventDTO;
import com.mr.global.client.gemini.GeminiStreamingClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

class MentorStreamingServiceTest {

    private final List<Runnable> tasks = new ArrayList<>();
    private GeminiStreamingClient geminiStreamingClient;
    private MentorQuestionService questionService;
    private MentorStreamingService streamingService;

    @BeforeEach
    void setUp() {
        geminiStreamingClient = mock(GeminiStreamingClient.class);
        questionService = mock(MentorQuestionService.class);
        TaskExecutor taskExecutor = tasks::add;
        streamingService = new MentorStreamingService(
                geminiStreamingClient,
                questionService,
                taskExecutor
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void stream_newGenerationCancelsPreviousGenerationBeforeNextChunk() {
        MentorQuestionService.PreparedQuestion previous = prepared("previous-token");
        MentorQuestionService.PreparedQuestion current = prepared("current-token");
        when(geminiStreamingClient.stream(anyString(), anyString(), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<String> chunkConsumer = invocation.getArgument(2);
                    chunkConsumer.accept("chunk");
                    return "answer";
                });

        streamingService.stream(previous);
        streamingService.stream(current);
        tasks.get(0).run();

        verify(questionService, never()).complete(any(), anyString(), anyString());
        verify(questionService, never()).fail(any(), anyString());
    }

    private MentorQuestionService.PreparedQuestion prepared(String generationToken) {
        return new MentorQuestionService.PreparedQuestion(
                1L,
                generationToken,
                "prompt",
                new MentorStreamEventDTO.Start(10L, 1L, null)
        );
    }
}
