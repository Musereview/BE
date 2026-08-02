package com.mr.global.file.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        Credentials credentials,
        String bucket,
        String region,
        Duration presignedUrlExpiration,
        Long maxFileSize,
        String keyPrefix,
        Set<String> allowedContentTypes
){
    public record Credentials(String accessKey, String secretKey) {}

    public String accessKey() {
        return credentials.accessKey();
    }

    public String secretKey() {
        return credentials.secretKey();
    }
}