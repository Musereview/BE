package com.mr.global.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisRequestSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 요청_직렬화시_AI_서버_스키마와_동일한_snake_case_필드명을_사용한다() throws Exception {
        AiAnalysisRequest request = new AiAnalysisRequest(
                new AiAnalysisRequest.Meta(120.0, List.of(4, 4),
                        new AiAnalysisRequest.Key("C", "major"), "jazz", "basic"),
                List.of(new AiAnalysisRequest.Chord(1, 1.0, "Dm7")),
                List.of(new AiAnalysisRequest.Note(0, AiAnalysisRequest.NoteType.NOTE_ON, 62, 90, 0.0))
        );

        JsonNode json = objectMapper.valueToTree(request);

        assertThat(json.path("meta").has("time_signature")).isTrue();
        assertThat(json.path("meta").has("timeSignature")).isFalse();
        assertThat(json.path("meta").has("level")).isTrue();
        assertThat(json.path("notes").get(0).has("timestamp_ms")).isTrue();
        assertThat(json.path("notes").get(0).has("timestampMs")).isFalse();
        assertThat(json.path("notes").get(0).has("onset_beats")).isFalse();
        assertThat(json.path("notes").get(0).has("duration_beats")).isFalse();

        assertThat(json.path("meta").path("bpm").asDouble()).isEqualTo(120.0);
        assertThat(json.path("meta").path("key").path("tonic").asText()).isEqualTo("C");
        assertThat(json.path("meta").path("key").path("mode").asText()).isEqualTo("major");
        assertThat(json.path("meta").path("level").asText()).isEqualTo("basic");
        assertThat(json.path("chords").get(0).path("bar").asInt()).isEqualTo(1);
        assertThat(json.path("chords").get(0).path("symbol").asText()).isEqualTo("Dm7");
        assertThat(json.path("notes").get(0).path("pitch").asInt()).isEqualTo(62);
        assertThat(json.path("notes").get(0).path("velocity").asInt()).isEqualTo(90);
        assertThat(json.path("notes").get(0).path("type").asText()).isEqualTo("NOTE_ON");
        assertThat(json.path("notes").get(0).path("timestamp_ms").asDouble()).isEqualTo(0.0);
    }
}
