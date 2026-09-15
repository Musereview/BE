package com.mr.domain.mentor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mr.domain.mentor.dto.res.MentorStreamEventDTO;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MentorSseConnectionManagerTest {

    @Test
    void open_sameSession_closesPreviousWithoutRecovery() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        AtomicInteger recoveryCount = new AtomicInteger();

        manager.open(1L, "previous-token", recoveryCount::incrementAndGet);
        manager.open(1L, "current-token", recoveryCount::incrementAndGet);

        assertThat(emitters.get(0).completeCount()).isOne();
        assertThat(emitters.get(1).completeCount()).isZero();
        assertThat(recoveryCount).hasValue(0);
    }

    @Test
    void open_sameSession_waitsForInFlightChunkBeforeReplacingConnection() throws Exception {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        MentorSseConnectionManager.Connection previous = manager.open(1L, "previous-token", () -> {
        });
        CountDownLatch chunkStarted = new CountDownLatch(1);
        CountDownLatch allowChunk = new CountDownLatch(1);
        emitters.get(0).blockNextSend(chunkStarted, allowChunk);

        Thread chunkThread = new Thread(() -> manager.sendChunk(previous, "chunk"));
        chunkThread.start();
        assertThat(chunkStarted.await(1, TimeUnit.SECONDS)).isTrue();

        AtomicReference<MentorSseConnectionManager.Connection> current = new AtomicReference<>();
        CountDownLatch replacementStarted = new CountDownLatch(1);
        CountDownLatch replacementCompleted = new CountDownLatch(1);
        Thread replacementThread = new Thread(() -> {
            replacementStarted.countDown();
            current.set(manager.open(1L, "current-token", () -> {
            }));
            replacementCompleted.countDown();
        });
        replacementThread.start();

        assertThat(replacementStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(replacementCompleted.await(100, TimeUnit.MILLISECONDS)).isFalse();
        allowChunk.countDown();
        chunkThread.join(1_000L);
        replacementThread.join(1_000L);

        assertThat(replacementCompleted.getCount()).isZero();
        assertThat(emitters.get(0).completeCount()).isOne();
        manager.sendChunk(current.get(), "current-chunk");
    }

    @Test
    void open_sameSession_waitsForInFlightCompletionBeforeReplacingConnection() throws Exception {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        MentorSseConnectionManager.Connection previous = manager.open(1L, "previous-token", () -> {
        });
        CountDownLatch completionStarted = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);

        Thread completionThread = new Thread(() -> manager.complete(previous, () -> {
            completionStarted.countDown();
            await(allowCompletion);
            return new MentorStreamEventDTO.Complete(null);
        }));
        completionThread.start();
        assertThat(completionStarted.await(1, TimeUnit.SECONDS)).isTrue();

        CountDownLatch replacementStarted = new CountDownLatch(1);
        CountDownLatch replacementCompleted = new CountDownLatch(1);
        Thread replacementThread = new Thread(() -> {
            replacementStarted.countDown();
            manager.open(1L, "current-token", () -> {
            });
            replacementCompleted.countDown();
        });
        replacementThread.start();

        assertThat(replacementStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(replacementCompleted.await(100, TimeUnit.MILLISECONDS)).isFalse();
        allowCompletion.countDown();
        completionThread.join(1_000L);
        replacementThread.join(1_000L);

        assertThat(replacementCompleted.getCount()).isZero();
        assertEvent(emitters.get(0), "complete", new MentorStreamEventDTO.Complete(null));
    }

    @Test
    void supersededConnection_rejectsStartChunkAndCompletion() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        MentorSseConnectionManager.Connection previous = manager.open(1L, "previous-token", () -> {
        });
        manager.open(1L, "current-token", () -> {
        });
        AtomicInteger completionCount = new AtomicInteger();

        assertThatThrownBy(() -> manager.sendStart(
                previous, new MentorStreamEventDTO.Start(10L, 1L, null)))
                .isInstanceOf(MentorSseConnectionManager.ConnectionTerminatedException.class);
        assertThatThrownBy(() -> manager.sendChunk(previous, "chunk"))
                .isInstanceOf(MentorSseConnectionManager.ConnectionTerminatedException.class);
        assertThatThrownBy(() -> manager.complete(previous, () -> {
            completionCount.incrementAndGet();
            return new MentorStreamEventDTO.Complete(null);
        })).isInstanceOf(MentorSseConnectionManager.ConnectionTerminatedException.class);

        assertThat(completionCount).hasValue(0);
        assertThat(emitters.get(0).sentEvents()).isEmpty();
    }

    @Test
    void closeSilently_alreadyTerminated_doesNotCompleteAgain() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        MentorSseConnectionManager.Connection connection = manager.open(1L, "token", () -> {
        });

        manager.closeSilently(connection);
        manager.closeSilently(connection);

        assertThat(emitters.get(0).completeCount()).isOne();
    }

    @Test
    void completion_previousRecoveryFinishesLate_doesNotRemoveCurrentConnection() throws Exception {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        CountDownLatch recoveryStarted = new CountDownLatch(1);
        CountDownLatch allowRecovery = new CountDownLatch(1);

        manager.open(1L, "previous-token", () -> {
            recoveryStarted.countDown();
            await(allowRecovery);
        });
        Thread previousCompletion = new Thread(emitters.get(0)::triggerCompletion);
        previousCompletion.start();
        assertThat(recoveryStarted.await(1, TimeUnit.SECONDS)).isTrue();

        manager.open(1L, "current-token", () -> {
        });
        allowRecovery.countDown();
        previousCompletion.join(1_000L);

        manager.open(1L, "next-token", () -> {
        });

        assertThat(emitters.get(1).completeCount()).isOne();
        assertThat(emitters.get(2).completeCount()).isZero();
    }

    @Test
    void timeout_followedByOtherCallbacks_recoversOnlyOnce() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        AtomicInteger recoveryCount = new AtomicInteger();

        manager.open(1L, "token", recoveryCount::incrementAndGet);
        TestSseEmitter emitter = emitters.get(0);

        emitter.triggerTimeout();
        emitter.triggerError();
        emitter.triggerCompletion();

        assertThat(recoveryCount).hasValue(1);
        assertThat(emitter.completeCount()).isOne();
    }

    @Test
    void error_recoversAndCompletesConnection() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        AtomicInteger recoveryCount = new AtomicInteger();

        manager.open(1L, "token", recoveryCount::incrementAndGet);
        TestSseEmitter emitter = emitters.get(0);

        emitter.triggerError();

        assertThat(recoveryCount).hasValue(1);
        assertThat(emitter.completeCount()).isOne();
    }

    @Test
    void completion_recoversWithoutCompletingAgain() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        AtomicInteger recoveryCount = new AtomicInteger();

        manager.open(1L, "token", recoveryCount::incrementAndGet);
        TestSseEmitter emitter = emitters.get(0);

        emitter.triggerCompletion();

        assertThat(recoveryCount).hasValue(1);
        assertThat(emitter.completeCount()).isZero();
    }

    @Test
    void sendEvents_preservesNamesAndPayloads() {
        List<TestSseEmitter> emitters = new ArrayList<>();
        MentorSseConnectionManager manager = manager(emitters);
        MentorStreamEventDTO.Start start = new MentorStreamEventDTO.Start(10L, 1L, null);
        MentorStreamEventDTO.Complete complete = new MentorStreamEventDTO.Complete(null);
        MentorStreamEventDTO.Error error = new MentorStreamEventDTO.Error(
                MentorErrorStatus.MENTOR_RESPONSE_GENERATION_FAILED.getCode(),
                MentorErrorStatus.MENTOR_RESPONSE_GENERATION_FAILED.getMessage()
        );

        MentorSseConnectionManager.Connection startConnection = manager.open(1L, "start-token", () -> {
        });
        manager.sendStart(startConnection, start);
        MentorSseConnectionManager.Connection chunkConnection = manager.open(2L, "chunk-token", () -> {
        });
        manager.sendChunk(chunkConnection, "chunk");
        MentorSseConnectionManager.Connection completeConnection = manager.open(3L, "complete-token", () -> {
        });
        manager.complete(completeConnection, () -> complete);
        MentorSseConnectionManager.Connection errorConnection = manager.open(4L, "error-token", () -> {
        });
        manager.fail(errorConnection, MentorErrorStatus.MENTOR_RESPONSE_GENERATION_FAILED);

        assertEvent(emitters.get(0), "start", start);
        assertEvent(emitters.get(1), "chunk", new MentorStreamEventDTO.Chunk("chunk"));
        assertEvent(emitters.get(2), "complete", complete);
        assertEvent(emitters.get(3), "error", error);
    }

    private MentorSseConnectionManager manager(List<TestSseEmitter> emitters) {
        return new MentorSseConnectionManager(timeout -> {
            TestSseEmitter emitter = new TestSseEmitter(timeout);
            emitters.add(emitter);
            return emitter;
        });
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void assertEvent(TestSseEmitter emitter, String name, Object payload) {
        List<Object> eventParts = emitter.sentEvents().get(0).build().stream()
                .map(DataWithMediaType::getData)
                .toList();

        assertThat(eventParts).containsExactly("event:" + name + "\ndata:", payload, "\n\n");
    }

    private static final class TestSseEmitter extends SseEmitter {

        private Runnable timeoutCallback;
        private Consumer<Throwable> errorCallback;
        private Runnable completionCallback;
        private final List<SseEventBuilder> sentEvents = new ArrayList<>();
        private CountDownLatch sendStarted;
        private CountDownLatch allowSend;
        private int completeCount;

        private TestSseEmitter(Long timeout) {
            super(timeout);
        }

        @Override
        public void onTimeout(Runnable callback) {
            timeoutCallback = callback;
        }

        @Override
        public void onError(Consumer<Throwable> callback) {
            errorCallback = callback;
        }

        @Override
        public void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            if (sendStarted != null) {
                sendStarted.countDown();
                awaitSend();
                sendStarted = null;
                allowSend = null;
            }
            sentEvents.add(builder);
        }

        @Override
        public synchronized void complete() {
            completeCount++;
        }

        private void triggerTimeout() {
            timeoutCallback.run();
        }

        private void triggerError() {
            errorCallback.accept(new IllegalStateException("connection error"));
        }

        private void triggerCompletion() {
            completionCallback.run();
        }

        private synchronized int completeCount() {
            return completeCount;
        }

        private synchronized List<SseEventBuilder> sentEvents() {
            return List.copyOf(sentEvents);
        }

        private synchronized void blockNextSend(CountDownLatch sendStarted, CountDownLatch allowSend) {
            this.sendStarted = sendStarted;
            this.allowSend = allowSend;
        }

        private void awaitSend() {
            try {
                if (!allowSend.await(1, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release SSE send.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
    }
}
