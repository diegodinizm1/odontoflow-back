package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.CreateAppointmentRequest;
import com.diego.odontoflowbackend.entity.dto.request.RescheduleAppointmentRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateAppointmentStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.AppointmentResponse;
import com.diego.odontoflowbackend.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Agenda e agendamentos")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Listar agendamentos no intervalo (semana/dia)")
    public List<AppointmentResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return appointmentService.listInRange(start, end);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar agendamento")
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reagendar consulta")
    public AppointmentResponse reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleAppointmentRequest request) {
        return appointmentService.reschedule(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status (cancelar/concluir)")
    public AppointmentResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return appointmentService.updateStatus(id, request);
    }
}
