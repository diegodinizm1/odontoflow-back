package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateChargeRequest(
        @NotNull UUID patientId,
        UUID appointmentId,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        LocalDate dueDate
) {}
