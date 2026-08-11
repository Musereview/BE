package com.mr.global.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mr.global.config.GeminiProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private MockRestServiceServer server;
    private GeminiClient client;

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
        client = new GeminiClient(builder.build(), properties);
    }

    @Test
    void generateReport_callsConfiguredModelAndCombinesTextParts() {
        server.expect(requestTo(BASE_URL
                        + "/v1beta/models/gemini-3-flash-preview:generateContent"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(4096))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.required[0]").value("summary"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.required[1]").value("report"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {
                              "parts": [
                                {"text":"# 리포트\\n"},
                                {"text":"내용"}
                              ]
                            }
                          }],
                          "usageMetadata": {
                            "promptTokenCount": 100,
                            "candidatesTokenCount": 50,
                            "totalTokenCount": 150,
                            "cachedContentTokenCount": 20
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        GeminiGenerationResult report = client.generateReport("system", "{}");

        assertThat(report.content()).isEqualTo("# 리포트\n내용");
        assertThat(report.promptTokens()).isEqualTo(100);
        assertThat(report.completionTokens()).isEqualTo(50);
        assertThat(report.totalTokens()).isEqualTo(150);
        assertThat(report.cacheHit()).isTrue();
        server.verify();
    }
}
