package com.diego.odontoflowbackend.entity.dto.request;

import com.diego.odontoflowbackend.entity.enums.Plan;
import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(
        @NotNull Plan plan
) {}
