package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.Invoice;
import com.diego.odontoflowbackend.entity.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String description,
        BigDecimal amount,
        InvoiceStatus status,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(i.getId(), i.getDescription(), i.getAmount(), i.getStatus(), i.getPaidAt(), i.getCreatedAt());
    }
}
