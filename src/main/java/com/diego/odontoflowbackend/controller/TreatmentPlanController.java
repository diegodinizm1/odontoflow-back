package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.CreateTreatmentPlanRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateTreatmentPlanStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.TreatmentPlanResponse;
import com.diego.odontoflowbackend.service.TreatmentPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/treatment-plans")
@RequiredArgsConstructor
@Tag(name = "Treatment Plans", description = "Treatment plans (budget) and item completion")
@SecurityRequirement(name = "bearerAuth")
public class TreatmentPlanController {

    private final TreatmentPlanService service;

    @GetMapping
    @Operation(summary = "List the patient's treatment plans")
    public List<TreatmentPlanResponse> list(@PathVariable UUID patientId) {
        return service.listByPatient(patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a treatment plan with items")
    public TreatmentPlanResponse create(@PathVariable UUID patientId,
                                        @Valid @RequestBody CreateTreatmentPlanRequest request) {
        return service.create(patientId, request);
    }

    @PatchMapping("/{planId}/status")
    @Operation(summary = "Change plan status (accept/cancel)")
    public TreatmentPlanResponse updateStatus(@PathVariable UUID patientId, @PathVariable UUID planId,
                                              @Valid @RequestBody UpdateTreatmentPlanStatusRequest request) {
        return service.updateStatus(planId, request);
    }

    @PostMapping("/{planId}/items/{itemId}/complete")
    @Operation(summary = "Complete an item — generates a pending charge in finances")
    public TreatmentPlanResponse completeItem(@PathVariable UUID patientId, @PathVariable UUID planId,
                                              @PathVariable UUID itemId) {
        return service.completeItem(planId, itemId);
    }
}
