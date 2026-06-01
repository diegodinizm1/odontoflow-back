package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.ClinicalRecord;
import com.diego.odontoflowbackend.entity.ToothState;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ClinicalRecordResponse(
        UUID id,
        Map<String, ToothState> odontogramData,
        String clinicalNotes,
        UUID appointmentId,
        String createdByName,
        LocalDateTime createdAt
) {
    public static ClinicalRecordResponse from(ClinicalRecord r) {
        return new ClinicalRecordResponse(
                r.getId(),
                r.getOdontogramData(),
                r.getClinicalNotes(),
                r.getAppointmentId(),
                r.getCreatedBy().getFullName(),
                r.getCreatedAt()
        );
    }
}
