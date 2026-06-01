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
@Tag(name = "Patients", description = "Gerenciamento de pacientes")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "Listar pacientes do tenant")
    public List<PatientResponse> list() {
        return patientService.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar paciente por ID")
    public PatientResponse getById(@PathVariable UUID id) {
        return patientService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo paciente")
    public PatientResponse create(@Valid @RequestBody CreatePatientRequest request) {
        return patientService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar paciente")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePatientRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover paciente")
    public void delete(@PathVariable UUID id) {
        patientService.delete(id);
    }
}
