package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.response.SubscriptionResponse;
import com.diego.odontoflowbackend.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Dev-only payment simulation ("stripe trigger" style). Disabled when the
 * application runs with the {@code prod} profile.
 */
@RestController
@RequestMapping("/billing/simulate")
@RequiredArgsConstructor
@Profile("!prod")
@Tag(name = "Billing Simulation", description = "Dev-only payment event triggers")
@SecurityRequirement(name = "bearerAuth")
public class BillingSimulationController {

    private final BillingService billingService;

    @PostMapping("/{invoiceId}/fail")
    @Operation(summary = "Simulate a failed payment for an invoice (-> PAST_DUE)")
    public SubscriptionResponse fail(@PathVariable UUID invoiceId) {
        return billingService.simulatePaymentEvent(invoiceId, false);
    }

    @PostMapping("/{invoiceId}/recover")
    @Operation(summary = "Simulate a recovered/successful payment for an invoice (-> ACTIVE)")
    public SubscriptionResponse recover(@PathVariable UUID invoiceId) {
        return billingService.simulatePaymentEvent(invoiceId, true);
    }
}
