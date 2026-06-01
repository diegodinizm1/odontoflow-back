package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UploadUrlRequest(
        @NotBlank String fileName,
        @NotBlank String contentType
) {}
