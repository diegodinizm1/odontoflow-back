package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.ClinicService;
import com.diego.odontoflowbackend.entity.enums.DentalSpecialty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicServiceResponse(
        UUID id,
        String name,
        DentalSpecialty category,
        int durationMinutes,
        BigDecimal price,
        List<UUID> dentistIds
) {
    public static PublicServiceResponse from(ClinicService s) {
        return new PublicServiceResponse(s.getId(), s.getName(), s.getCategory(),
                s.getDurationMinutes(), s.getPrice(), List.copyOf(s.getDentistIds()));
    }
}
