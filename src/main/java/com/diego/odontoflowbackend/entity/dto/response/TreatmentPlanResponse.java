package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.TreatmentPlan;
import com.diego.odontoflowbackend.entity.enums.TreatmentPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TreatmentPlanResponse(
        UUID id,
        String title,
        TreatmentPlanStatus status,
        BigDecimal total,
        String createdByName,
        LocalDateTime createdAt,
        List<TreatmentItemResponse> items
) {
    public static TreatmentPlanResponse from(TreatmentPlan p) {
        return new TreatmentPlanResponse(
                p.getId(),
                p.getTitle(),
                p.getStatus(),
                p.total(),
                p.getCreatedBy().getFullName(),
                p.getCreatedAt(),
                p.getItems().stream()
                        .sorted((a, b) -> a.getCreatedAt() != null && b.getCreatedAt() != null
                                ? a.getCreatedAt().compareTo(b.getCreatedAt()) : 0)
                        .map(TreatmentItemResponse::from)
                        .toList()
        );
    }
}
