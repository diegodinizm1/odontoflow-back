package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.CreatePatientRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdatePatientRequest;
import com.diego.odontoflowbackend.entity.dto.response.PatientResponse;
import com.diego.odontoflowbackend.service.PatientService;
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
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "List patients of the tenant")
    public List<PatientResponse> list() {
        return patientService.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public PatientResponse getById(@PathVariable UUID id) {
        return patientService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new patient")
    public PatientResponse create(@Valid @RequestBody CreatePatientRequest request) {
        return patientService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePatientRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete patient")
    public void delete(@PathVariable UUID id) {
        patientService.delete(id);
    }
}
