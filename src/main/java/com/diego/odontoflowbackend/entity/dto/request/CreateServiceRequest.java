package com.diego.odontoflowbackend.entity.dto.request;

import com.diego.odontoflowbackend.entity.enums.DentalSpecialty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateServiceRequest(
        @NotBlank String name,
        @NotNull DentalSpecialty category,
        @NotNull @Min(10) @Max(480) Integer durationMinutes,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        List<UUID> dentistIds
) {}
