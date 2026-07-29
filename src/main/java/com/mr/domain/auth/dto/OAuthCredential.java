package com.mr.domain.auth.dto;

public record OAuthCredential(
        CredentialType type,
        String value
) {
    public enum CredentialType {
        AUTHORIZATION_CODE,
        ACCESS_TOKEN
    }
}
