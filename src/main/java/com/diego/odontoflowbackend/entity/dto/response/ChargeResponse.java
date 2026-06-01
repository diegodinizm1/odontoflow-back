package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.Charge;
import com.diego.odontoflowbackend.entity.enums.ChargeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChargeResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID appointmentId,
        String description,
        BigDecimal amount,
        ChargeStatus status,
        LocalDate dueDate,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static ChargeResponse from(Charge c) {
        return new ChargeResponse(
                c.getId(),
                c.getPatient().getId(),
                c.getPatient().getFullName(),
                c.getAppointmentId(),
                c.getDescription(),
                c.getAmount(),
                c.getStatus(),
                c.getDueDate(),
                c.getPaidAt(),
                c.getCreatedAt()
        );
    }
}
