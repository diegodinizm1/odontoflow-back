package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.ClinicService;
import com.diego.odontoflowbackend.entity.enums.DentalSpecialty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String name,
        DentalSpecialty category,
        int durationMinutes,
        BigDecimal price,
        List<UUID> dentistIds,
        boolean active
) {
    public static ServiceResponse from(ClinicService s) {
        return new ServiceResponse(s.getId(), s.getName(), s.getCategory(), s.getDurationMinutes(),
                s.getPrice(), List.copyOf(s.getDentistIds()), s.isActive());
    }
}
