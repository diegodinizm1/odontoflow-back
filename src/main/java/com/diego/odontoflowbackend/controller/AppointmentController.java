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
@Tag(name = "Appointments", description = "Scheduling and appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "List appointments in a range (dentists see only their own; receptionists may filter by dentistId)")
    public List<AppointmentResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) UUID dentistId) {
        return appointmentService.listInRange(start, end, dentistId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create appointment")
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reschedule appointment")
    public AppointmentResponse reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleAppointmentRequest request) {
        return appointmentService.reschedule(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change status (cancel/complete)")
    public AppointmentResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return appointmentService.updateStatus(id, request);
    }
}
