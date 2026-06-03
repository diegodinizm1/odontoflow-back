package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.CreateBookingRequest;
import com.diego.odontoflowbackend.entity.dto.response.AvailabilityResponse;
import com.diego.odontoflowbackend.entity.dto.response.BookingConfirmationResponse;
import com.diego.odontoflowbackend.entity.dto.response.PublicClinicResponse;
import com.diego.odontoflowbackend.service.PublicBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/** Public, unauthenticated online-booking endpoints scoped by the clinic's slug. */
@RestController
@RequestMapping("/public/clinics/{slug}")
@RequiredArgsConstructor
@Tag(name = "Public booking", description = "Patient-facing online scheduling (no authentication)")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    @GetMapping
    @Operation(summary = "Public clinic profile (name + bookable dentists)")
    public PublicClinicResponse clinic(@PathVariable String slug) {
        return publicBookingService.clinic(slug);
    }

    @GetMapping("/availability")
    @Operation(summary = "Free start times for a dentist on a given day")
    public AvailabilityResponse availability(
            @PathVariable String slug,
            @RequestParam UUID dentistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return publicBookingService.availability(slug, dentistId, date);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request an appointment online (created as PENDING for the clinic to confirm)")
    public BookingConfirmationResponse book(
            @PathVariable String slug,
            @Valid @RequestBody CreateBookingRequest request) {
        return publicBookingService.book(slug, request);
    }
}
