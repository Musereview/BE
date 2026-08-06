package com.mr.global.security.jwt;

public enum JwtValidationResult {
    VALID,
    EXPIRED,
    INVALID_SIGNATURE,
    MALFORMED,
    INVALID_TYPE,
    UNSUPPORTED,
    INVALID
}
