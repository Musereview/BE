package com.mr.global.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.global.apipayload.domain.AiServerErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.config.AiServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServerClientTest {

    private static final String BASE_URL = "http://localhost:8000";

    private MockRestServiceServer mockServer;
    private AiServerClient client;

    @BeforeEach
    void setUp() {
        AiServerProperties properties = new AiServerProperties(
                BASE_URL, Duration.ofSeconds(3), Duration.ofSeconds(30),
                new AiServerProperties.Endpoints("/analyze"));

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AiServerClient(builder.build(), properties);
    }

    private AiAnalysisRequest sampleRequest() {
        return new AiAnalysisRequest(
                new AiAnalysisRequest.Meta(120.0, List.of(4, 4),
                        new AiAnalysisRequest.Key("C", "major"), "jazz"),
                List.of(new AiAnalysisRequest.Chord(1, 1.0, "Dm7")),
                List.of(new AiAnalysisRequest.Note(0, 62, 0.0, 1.0, 90))
        );
    }

    @Test
    void 정상_응답이면_AI_서버가_반환한_JSON을_그대로_돌려준다() {
        mockServer.expect(requestTo(BASE_URL + "/analyze"))
                .andRespond(withSuccess("{\"scores\":{\"final_score\":80.5}}", MediaType.APPLICATION_JSON));

        JsonNode result = client.requestAnalysis(sampleRequest());

        assertThat(result.path("scores").path("final_score").asDouble()).isEqualTo(80.5);
    }

    @Test
    void AI_서버가_400을_반환하면_RESPONSE_ERROR로_변환된다() {
        mockServer.expect(requestTo(BASE_URL + "/analyze"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"detail\":\"코드 파싱 실패\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.requestAnalysis(sampleRequest()))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AiServerErrorStatus.RESPONSE_ERROR));
    }

    @Test
    void AI_서버가_500을_반환하면_RESPONSE_ERROR로_변환된다() {
        mockServer.expect(requestTo(BASE_URL + "/analyze"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"detail\":\"알 수 없는 오류\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.requestAnalysis(sampleRequest()))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AiServerErrorStatus.RESPONSE_ERROR));
    }

    @Test
    void 연결이_거부되면_CONNECTION_FAILED로_변환된다() {
        mockServer.expect(requestTo(BASE_URL + "/analyze"))
                .andRespond(request -> {
                    throw new ConnectException("Connection refused");
                });

        assertThatThrownBy(() -> client.requestAnalysis(sampleRequest()))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AiServerErrorStatus.CONNECTION_FAILED));
    }

    @Test
    void 응답이_지연되어_타임아웃되면_TIMEOUT으로_변환된다() {
        mockServer.expect(requestTo(BASE_URL + "/analyze"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThatThrownBy(() -> client.requestAnalysis(sampleRequest()))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AiServerErrorStatus.TIMEOUT));
    }
}
