package com.mr.global.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.global.apipayload.domain.AiServerErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.config.AiServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final RestClient aiServerRestClient;
    private final AiServerProperties properties;

    public JsonNode requestAnalysis(AiAnalysisRequest request) {
        try {
            JsonNode response = aiServerRestClient.post()
                    .uri(properties.endpoints().analyze())
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new GeneralException(AiServerErrorStatus.INVALID_RESPONSE);
            }
            return response;
        } catch (ResourceAccessException e) {
            throw new GeneralException(hasTimeoutCause(e) ? AiServerErrorStatus.TIMEOUT : AiServerErrorStatus.CONNECTION_FAILED);
        } catch (HttpStatusCodeException e) {
            log.warn("AI 서버가 오류 응답을 반환했습니다. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(AiServerErrorStatus.RESPONSE_ERROR);
        } catch (RestClientException e) {
            throw new GeneralException(AiServerErrorStatus.INVALID_RESPONSE);
        }
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
