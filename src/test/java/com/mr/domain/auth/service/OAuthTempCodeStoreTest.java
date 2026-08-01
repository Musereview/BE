package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.auth.entity.enums.SocialType;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OAuthTempCodeStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OAuthTempCodeStore tempCodeStore;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        tempCodeStore = new OAuthTempCodeStore(redisTemplate, new ObjectMapper(), Duration.ofMinutes(2));
    }

    @Test
    @DisplayName("임시 코드는 원문이 아닌 해시 키로 TTL과 함께 저장")
    void save_hashesKeyAndAppliesTtl() {
        OAuthTempCodeStore.TempExchangeData data = data();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        tempCodeStore.save("raw-temp-code", data);

        verify(valueOperations).set(keyCaptor.capture(), anyString(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(2)));
        assertThat(keyCaptor.getValue()).startsWith("oauth:temp-code:");
        assertThat(keyCaptor.getValue()).doesNotContain("raw-temp-code");
    }

    @Test
    @DisplayName("임시 코드는 GETDEL로 원자적으로 한 번만 소비")
    void consume_getsAndDeletesAtomically() throws Exception {
        OAuthTempCodeStore.TempExchangeData data = data();
        String json = new ObjectMapper().writeValueAsString(data);
        given(valueOperations.getAndDelete(anyString())).willReturn(json);

        Optional<OAuthTempCodeStore.TempExchangeData> result = tempCodeStore.consume("raw-temp-code");

        assertThat(result).contains(data);
        verify(valueOperations).getAndDelete(anyString());
    }

    private OAuthTempCodeStore.TempExchangeData data() {
        return new OAuthTempCodeStore.TempExchangeData(
                1L,
                SocialType.KAKAO,
                "social-id",
                "https://example.com/profile.png",
                "device"
        );
    }
}
