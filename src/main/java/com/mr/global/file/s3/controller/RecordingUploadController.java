package com.mr.global.file.s3.controller;

import com.mr.global.apipayload.ApiResponse;
import com.mr.global.file.s3.dto.req.RecordingPresignedUrlRequest;
import com.mr.global.file.s3.dto.req.RecordingUploadCompleteRequest;
import com.mr.global.file.s3.dto.res.RecordingPresignedUrlResponse;
import com.mr.global.file.s3.dto.res.RecordingUploadCompleteResponse;
import com.mr.global.file.s3.service.RecordingUploadService;
import com.mr.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class RecordingUploadController {

    private final RecordingUploadService recordingUploadService;

    @PostMapping("/presigned-url")
    public ApiResponse<RecordingPresignedUrlResponse> createPresignedUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecordingPresignedUrlRequest request
    ) {
        Long userId = userDetails.getUserId();

        RecordingPresignedUrlResponse response = recordingUploadService.createPresignedUrl(userId, request);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/complete")
    public ApiResponse<RecordingUploadCompleteResponse> completeUpload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecordingUploadCompleteRequest request
    ) {

        Long userId = userDetails.getUserId();

        RecordingUploadCompleteResponse response = recordingUploadService.completeUpload(userId, request);
        return ApiResponse.onSuccess(response);
    }
}
