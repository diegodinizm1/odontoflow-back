package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreatePatientRequest(
        @NotBlank String fullName,
        LocalDate dateOfBirth,
        String medicalAlerts
) {}
