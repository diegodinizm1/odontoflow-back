package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.response.PatientAuditResponse;
import com.diego.odontoflowbackend.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Change history (audit trail) for compliance")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Patient change history (Hibernate Envers)")
    public List<PatientAuditResponse> patientHistory(@PathVariable UUID patientId) {
        return auditService.patientHistory(patientId);
    }
}
