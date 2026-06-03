package com.diego.odontoflowbackend.entity.dto.response;

import java.util.List;

/** Public profile of a clinic shown on the online-booking page (no sensitive data). */
public record PublicClinicResponse(
        String clinicName,
        String publicSlug,
        List<PublicDentistResponse> dentists
) {}
