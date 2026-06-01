package com.diego.odontoflowbackend.entity.dto.request;

import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
        @NotNull AppointmentStatus status
) {}
