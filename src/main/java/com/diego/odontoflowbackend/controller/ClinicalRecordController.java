package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.ToothState;
import com.diego.odontoflowbackend.entity.dto.request.CreateClinicalRecordRequest;
import com.diego.odontoflowbackend.entity.dto.response.ClinicalRecordResponse;
import com.diego.odontoflowbackend.service.ClinicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}")
@RequiredArgsConstructor
@Tag(name = "Clinical Records", description = "Patient record, odontogram and clinical evolution")
@SecurityRequirement(name = "bearerAuth")
public class ClinicalRecordController {

    private final ClinicalRecordService recordService;

    @GetMapping("/records")
    @Operation(summary = "Patient clinical evolution history")
    public List<ClinicalRecordResponse> list(@PathVariable UUID patientId) {
        return recordService.listByPatient(patientId);
    }

    @GetMapping("/odontogram")
    @Operation(summary = "Current (latest) odontogram state")
    public Map<String, ToothState> latestOdontogram(@PathVariable UUID patientId) {
        return recordService.latestOdontogram(patientId);
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DENTIST')")
    @Operation(summary = "Save clinical evolution + odontogram (dentists only)")
    public ClinicalRecordResponse create(@PathVariable UUID patientId,
                                         @Valid @RequestBody CreateClinicalRecordRequest request) {
        return recordService.create(patientId, request);
    }
}
