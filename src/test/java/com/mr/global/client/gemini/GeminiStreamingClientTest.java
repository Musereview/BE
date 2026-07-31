package com.mr.global.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.global.config.GeminiProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiStreamingClientTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private MockRestServiceServer server;
    private GeminiStreamingClient client;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties(
                BASE_URL,
                "test-key",
                "gemini-3-flash-preview",
                Duration.ofSeconds(5),
                Duration.ofSeconds(60)
        );
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiStreamingClient(builder.build(), properties, new ObjectMapper());
    }

    @Test
    void stream_forwardsIncrementalChunksAndReturnsCombinedAnswer() {
        server.expect(requestTo(BASE_URL
                        + "/v1beta/models/gemini-3-flash-preview:streamGenerateContent?alt=sse"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess("""
                        data: {"candidates":[{"content":{"parts":[{"text":"첫 문장. "}]}}]}

                        data: {"candidates":[{"content":{"parts":[{"text":"둘째 문장."}]}}]}

                        """, MediaType.TEXT_EVENT_STREAM));
        List<String> chunks = new ArrayList<>();

        String answer = client.stream("system", "prompt", chunks::add);

        assertThat(chunks).containsExactly("첫 문장. ", "둘째 문장.");
        assertThat(answer).isEqualTo("첫 문장. 둘째 문장.");
        server.verify();
    }
}
