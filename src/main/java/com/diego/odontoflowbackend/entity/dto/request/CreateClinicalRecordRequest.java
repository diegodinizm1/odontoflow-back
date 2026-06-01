package com.diego.odontoflowbackend.entity.dto.request;

import com.diego.odontoflowbackend.entity.ToothState;

import java.util.Map;
import java.util.UUID;

public record CreateClinicalRecordRequest(
        Map<String, ToothState> odontogramData,
        String clinicalNotes,
        UUID appointmentId
) {}
