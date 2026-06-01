package com.diego.odontoflowbackend.entity.dto.request;

import com.diego.odontoflowbackend.entity.enums.ChargeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateChargeStatusRequest(
        @NotNull ChargeStatus status
) {}
