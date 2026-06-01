package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.PatientFile;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientFileResponse(
        UUID id,
        String fileName,
        String contentType,
        String downloadUrl,
        String uploadedByName,
        LocalDateTime createdAt
) {
    public static PatientFileResponse from(PatientFile f, String downloadUrl) {
        return new PatientFileResponse(
                f.getId(),
                f.getFileName(),
                f.getContentType(),
                downloadUrl,
                f.getUploadedBy().getFullName(),
                f.getCreatedAt()
        );
    }
}
