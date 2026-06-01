package com.diego.odontoflowbackend.entity.dto.response;

import java.time.LocalDateTime;

public record PatientAuditResponse(
        Number revision,
        String revisionType,   // ADD, MOD, DEL
        String changedByUserId,
        LocalDateTime changedAt,
        String fullName,
        String medicalAlerts
) {}
