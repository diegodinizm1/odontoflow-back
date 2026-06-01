package com.diego.odontoflowbackend.entity.dto.response;

import java.util.UUID;

public record UploadUrlResponse(
        UUID fileId,
        String uploadUrl,
        String key
) {}
