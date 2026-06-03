package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank String name,
        @NotNull @Min(10) @Max(480) Integer durationMinutes,
        @NotNull @DecimalMin("0.0") BigDecimal price
) {}
