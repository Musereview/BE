package com.mr.domain.auth.exception;

import com.mr.global.apipayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class OAuthExceptionMapper {

    public GeneralException map(Exception e, String provider) {
        if (e instanceof GeneralException ge) {
            return ge;
        }
        if (e instanceof HttpClientErrorException clientErr) {
            log.warn("{} OAuth 클라이언트 인증 오류 (status={}): {}", provider, clientErr.getStatusCode(), clientErr.getMessage());
            return new GeneralException(AuthErrorStatus.OAUTH_CLIENT_ERROR);
        }
        if (e instanceof HttpServerErrorException serverErr) {
            log.error("{} OAuth 서버 오류 (status={}): {}", provider, serverErr.getStatusCode(), serverErr.getMessage());
            return new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }
        if (e instanceof ResourceAccessException netErr) {
            log.error("{} OAuth 타임아웃/네트워크 통신 오류: {}", provider, netErr.getMessage());
            return new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }
        if (e instanceof RestClientResponseException rcre) {
            log.error("{} OAuth HTTP 응답 예외 (status={}): {}", provider, rcre.getStatusCode(), rcre.getMessage());
            if (rcre.getStatusCode().is4xxClientError()) {
                return new GeneralException(AuthErrorStatus.OAUTH_CLIENT_ERROR);
            } else {
                return new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
            }
        }
        log.error("{} OAuth 사용자 정보 처리 중 예외 발생", provider, e);
        return new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
    }
}
