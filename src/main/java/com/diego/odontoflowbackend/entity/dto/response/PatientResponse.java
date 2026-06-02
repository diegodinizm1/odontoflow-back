package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String fullName,
        String phone,
        LocalDate dateOfBirth,
        String medicalAlerts,
        LocalDateTime createdAt
) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getId(), p.getFullName(), p.getPhone(), p.getDateOfBirth(), p.getMedicalAlerts(), p.getCreatedAt());
    }
}
