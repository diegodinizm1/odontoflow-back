package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateTreatmentPlanRequest(
        @NotBlank String title,
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotBlank String description,
            String tooth,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
    ) {}
}
