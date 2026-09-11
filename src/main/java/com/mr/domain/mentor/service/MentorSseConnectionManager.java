package com.mr.domain.mentor.service;

import com.mr.domain.mentor.dto.res.MentorStreamEventDTO;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MentorSseConnectionManager {

    private static final long SSE_TIMEOUT_MS = 70_000L;

    private final Map<Long, Connection> activeConnections = new ConcurrentHashMap<>();
    private final LongFunction<SseEmitter> emitterFactory;

    public MentorSseConnectionManager() {
        this(SseEmitter::new);
    }

    MentorSseConnectionManager(LongFunction<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public Connection open(Long sessionId, String generationToken, Runnable recovery) {
        SseEmitter emitter = emitterFactory.apply(SSE_TIMEOUT_MS);
        Connection connection = new Connection(sessionId, generationToken, emitter, recovery);
        emitter.onTimeout(() -> recover(connection, true));
        emitter.onError(exception -> recover(connection, true));
        emitter.onCompletion(() -> recover(connection, false));

        Connection previous = activeConnections.put(sessionId, connection);
        if (previous != null) {
            closeSuperseded(previous);
        }
        return connection;
    }

    public boolean isTerminated(Connection connection) {
        return connection.isTerminated();
    }

    public void sendStart(Connection connection, MentorStreamEventDTO.Start event) {
        send(connection.emitter(), "start", event);
    }

    public void sendChunk(Connection connection, String chunk) {
        synchronized (connection) {
            ensureActive(connection);
            send(connection.emitter(), "chunk", new MentorStreamEventDTO.Chunk(chunk));
        }
    }

    public void complete(Connection connection, Supplier<MentorStreamEventDTO.Complete> completion) {
        MentorStreamEventDTO.Complete event;
        synchronized (connection) {
            ensureActive(connection);
            event = completion.get();
            connection.terminate();
        }
        remove(connection);
        try {
            send(connection.emitter(), "complete", event);
        } catch (RuntimeException exception) {
            log.debug("AI mentor completion event could not be delivered. sessionId={}", connection.sessionId());
        } finally {
            connection.emitter().complete();
        }
    }

    public void fail(Connection connection, MentorErrorStatus errorStatus) {
        if (!terminateAndRecover(connection)) {
            return;
        }
        remove(connection);
        sendError(connection.emitter(), errorStatus);
    }

    public void recover(Connection connection, boolean notifyClient) {
        if (!terminateAndRecover(connection)) {
            return;
        }
        remove(connection);
        if (notifyClient) {
            sendError(connection.emitter(), MentorErrorStatus.MENTOR_RESPONSE_GENERATION_FAILED);
        }
    }

    public void closeSilently(Connection connection) {
        synchronized (connection) {
            connection.terminate();
        }
        remove(connection);
        connection.emitter().complete();
    }

    private boolean terminateAndRecover(Connection connection) {
        synchronized (connection) {
            if (!connection.terminate()) {
                return false;
            }
            connection.recovery().run();
            return true;
        }
    }

    private void closeSuperseded(Connection connection) {
        synchronized (connection) {
            if (!connection.terminate()) {
                return;
            }
        }
        remove(connection);
        connection.emitter().complete();
    }

    private void ensureActive(Connection connection) {
        if (connection.isTerminated()) {
            throw new ConnectionTerminatedException();
        }
    }

    private void remove(Connection connection) {
        activeConnections.remove(connection.sessionId(), connection);
    }

    private void send(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void sendError(SseEmitter emitter, MentorErrorStatus status) {
        try {
            send(emitter, "error", new MentorStreamEventDTO.Error(status.getCode(), status.getMessage()));
        } catch (RuntimeException ignored) {
            log.debug("AI mentor SSE error event could not be delivered.");
        } finally {
            emitter.complete();
        }
    }

    public static final class Connection {

        private final Long sessionId;
        private final String generationToken;
        private final SseEmitter emitter;
        private final Runnable recovery;
        private volatile boolean terminated;

        private Connection(Long sessionId, String generationToken, SseEmitter emitter, Runnable recovery) {
            this.sessionId = sessionId;
            this.generationToken = generationToken;
            this.emitter = emitter;
            this.recovery = recovery;
        }

        public SseEmitter emitter() {
            return emitter;
        }

        Long sessionId() {
            return sessionId;
        }

        String generationToken() {
            return generationToken;
        }

        Runnable recovery() {
            return recovery;
        }

        private boolean isTerminated() {
            return terminated;
        }

        private boolean terminate() {
            if (terminated) {
                return false;
            }
            terminated = true;
            return true;
        }
    }

    public static final class ConnectionTerminatedException extends RuntimeException {
    }
}
