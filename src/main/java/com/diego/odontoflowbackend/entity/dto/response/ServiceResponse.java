package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.ClinicService;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String name,
        int durationMinutes,
        BigDecimal price,
        boolean active
) {
    public static ServiceResponse from(ClinicService s) {
        return new ServiceResponse(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice(), s.isActive());
    }
}
