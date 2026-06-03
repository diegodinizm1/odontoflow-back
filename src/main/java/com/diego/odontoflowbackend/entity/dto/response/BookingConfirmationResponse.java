package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Returned to the patient after requesting an online appointment. */
public record BookingConfirmationResponse(
        UUID appointmentId,
        String dentistName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status
) {}
