package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.ClinicService;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicServiceResponse(
        UUID id,
        String name,
        int durationMinutes,
        BigDecimal price
) {
    public static PublicServiceResponse from(ClinicService s) {
        return new PublicServiceResponse(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice());
    }
}
