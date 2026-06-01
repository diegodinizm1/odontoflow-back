package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID dentistId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPatient().getFullName(),
                a.getDentistId(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus()
        );
    }
}
