package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.SubscribeRequest;
import com.diego.odontoflowbackend.entity.dto.response.InvoiceResponse;
import com.diego.odontoflowbackend.entity.dto.response.PlanResponse;
import com.diego.odontoflowbackend.entity.dto.response.SubscriptionResponse;
import com.diego.odontoflowbackend.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Subscription plans and invoices (SaaS billing)")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    @Operation(summary = "List available plans")
    public List<PlanResponse> plans() {
        return billingService.plans();
    }

    @GetMapping("/subscription")
    @Operation(summary = "Current tenant subscription")
    public SubscriptionResponse subscription() {
        return billingService.currentSubscription();
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to / change plan")
    public SubscriptionResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
        return billingService.subscribe(request.plan());
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel subscription")
    public SubscriptionResponse cancel() {
        return billingService.cancel();
    }

    @GetMapping("/invoices")
    @Operation(summary = "List tenant invoices")
    public List<InvoiceResponse> invoices() {
        return billingService.invoices();
    }
}
