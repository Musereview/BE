package com.mr.global.client.ai;

import com.mr.global.apipayload.domain.AiServerErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.config.AiServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final RestClient aiServerRestClient;
    private final AiServerProperties properties;

    public AiAnalysisResponse requestAnalysis(AiAnalysisRequest request) {
        try {
            return aiServerRestClient.post()
                    .uri(properties.endpoints().analyze())
                    .body(request)
                    .retrieve()
                    .body(AiAnalysisResponse.class);
        } catch (ResourceAccessException e) {
            boolean isTimeout = e.getCause() instanceof java.net.SocketTimeoutException;
            throw new GeneralException(isTimeout ? AiServerErrorStatus.TIMEOUT : AiServerErrorStatus.CONNECTION_FAILED);
        } catch (HttpStatusCodeException e) {
            throw new GeneralException(AiServerErrorStatus.RESPONSE_ERROR);
        } catch (RestClientException e) {
            throw new GeneralException(AiServerErrorStatus.INVALID_RESPONSE);
        }
    }
}
