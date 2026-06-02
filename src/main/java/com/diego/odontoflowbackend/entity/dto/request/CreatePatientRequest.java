package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreatePatientRequest(
        @NotBlank String fullName,
        String phone,
        LocalDate dateOfBirth,
        String medicalAlerts
) {}
