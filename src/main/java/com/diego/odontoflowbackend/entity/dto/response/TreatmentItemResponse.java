package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.TreatmentItem;
import com.diego.odontoflowbackend.entity.enums.TreatmentItemStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TreatmentItemResponse(
        UUID id,
        String description,
        String tooth,
        BigDecimal amount,
        TreatmentItemStatus status,
        UUID chargeId
) {
    public static TreatmentItemResponse from(TreatmentItem i) {
        return new TreatmentItemResponse(i.getId(), i.getDescription(), i.getTooth(), i.getAmount(), i.getStatus(), i.getChargeId());
    }
}
