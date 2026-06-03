package com.diego.odontoflowbackend.entity.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Free start times (HH:mm) for a given dentist on a given day. */
public record AvailabilityResponse(
        UUID dentistId,
        LocalDate date,
        List<String> slots
) {}
