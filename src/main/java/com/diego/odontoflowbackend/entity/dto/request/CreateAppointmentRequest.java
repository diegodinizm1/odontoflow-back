package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID patientId,
        UUID dentistId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {}
