package com.diego.odontoflowbackend.entity.dto.request;

import com.diego.odontoflowbackend.entity.enums.TreatmentPlanStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTreatmentPlanStatusRequest(
        @NotNull TreatmentPlanStatus status
) {}
