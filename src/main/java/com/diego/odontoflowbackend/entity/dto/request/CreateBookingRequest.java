package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** A patient's online appointment request (no authentication). */
public record CreateBookingRequest(
        @NotNull UUID dentistId,
        @NotNull UUID serviceId,
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        @NotBlank String patientName,
        @NotBlank String patientPhone
) {}
