package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.CreateChargeRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateChargeStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.ChargeResponse;
import com.diego.odontoflowbackend.entity.dto.response.FinancialSummaryResponse;
import com.diego.odontoflowbackend.service.ChargeService;
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
@RequestMapping("/charges")
@RequiredArgsConstructor
@Tag(name = "Charges", description = "Clinic finances: per-appointment charges and revenue")
@SecurityRequirement(name = "bearerAuth")
public class ChargeController {

    private final ChargeService chargeService;

    @GetMapping
    @Operation(summary = "List charges of the tenant")
    public List<ChargeResponse> list() {
        return chargeService.list();
    }

    @GetMapping("/summary")
    @Operation(summary = "Financial summary (paid this month, total pending)")
    public FinancialSummaryResponse summary(@RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month) {
        if (year != null && month != null) {
            return chargeService.summary(year, month);
        }
        return chargeService.summaryForCurrentMonth();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a charge")
    public ChargeResponse create(@Valid @RequestBody CreateChargeRequest request) {
        return chargeService.create(request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change charge status (mark paid/canceled)")
    public ChargeResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateChargeStatusRequest request) {
        return chargeService.updateStatus(id, request);
    }
}
