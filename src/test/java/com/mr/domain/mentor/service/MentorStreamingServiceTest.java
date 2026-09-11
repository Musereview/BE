package com.mr.domain.mentor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr.domain.mentor.dto.res.MentorStreamEventDTO;
import com.mr.global.client.gemini.GeminiStreamingClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MentorStreamingServiceTest {

    private final List<Runnable> tasks = new ArrayList<>();
    private GeminiStreamingClient geminiStreamingClient;
    private MentorQuestionService questionService;
    private MentorStreamingService streamingService;
    private List<FailingChunkSseEmitter> emitters;

    @BeforeEach
    void setUp() {
        geminiStreamingClient = mock(GeminiStreamingClient.class);
        questionService = mock(MentorQuestionService.class);
        emitters = new ArrayList<>();
        TaskExecutor taskExecutor = tasks::add;
        MentorSseConnectionManager connectionManager = new MentorSseConnectionManager(timeout -> {
            FailingChunkSseEmitter emitter = new FailingChunkSseEmitter(timeout, emitters.isEmpty());
            emitters.add(emitter);
            return emitter;
        });
        streamingService = new MentorStreamingService(
                geminiStreamingClient,
                questionService,
                taskExecutor,
                connectionManager
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

    @Test
    @SuppressWarnings("unchecked")
    void stream_chunkSendFails_recoversOnceAndRemovesConnection() {
        MentorQuestionService.PreparedQuestion failed = prepared("failed-token");
        when(geminiStreamingClient.stream(anyString(), anyString(), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<String> chunkConsumer = invocation.getArgument(2);
                    chunkConsumer.accept("chunk");
                    return "answer";
                });

        streamingService.stream(failed);
        tasks.get(0).run();
        emitters.get(0).triggerCompletion();
        streamingService.stream(prepared("current-token"));

        verify(questionService).fail(1L, "failed-token");
        assertThat(emitters.get(0).completeCount()).isZero();
    }

    private MentorQuestionService.PreparedQuestion prepared(String generationToken) {
        return new MentorQuestionService.PreparedQuestion(
                1L,
                generationToken,
                "prompt",
                new MentorStreamEventDTO.Start(10L, 1L, null)
        );
    }

    private static final class FailingChunkSseEmitter extends SseEmitter {

        private final boolean failChunk;
        private Runnable completionCallback;
        private int sendCount;
        private int completeCount;

        private FailingChunkSseEmitter(Long timeout, boolean failChunk) {
            super(timeout);
            this.failChunk = failChunk;
        }

        @Override
        public void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            sendCount++;
            if (failChunk && sendCount == 2) {
                throw new IOException("connection closed");
            }
        }

        @Override
        public synchronized void complete() {
            completeCount++;
        }

        private void triggerCompletion() {
            completionCallback.run();
        }

        private synchronized int completeCount() {
            return completeCount;
        }
    }
}
