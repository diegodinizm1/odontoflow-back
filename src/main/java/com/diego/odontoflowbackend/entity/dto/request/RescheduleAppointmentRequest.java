package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {}
