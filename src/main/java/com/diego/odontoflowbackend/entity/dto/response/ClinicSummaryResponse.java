package com.diego.odontoflowbackend.entity.dto.response;

/** A clinic card in the public directory (marketplace). */
public record ClinicSummaryResponse(
        String clinicName,
        String publicSlug,
        long dentistCount,
        long serviceCount
) {}
