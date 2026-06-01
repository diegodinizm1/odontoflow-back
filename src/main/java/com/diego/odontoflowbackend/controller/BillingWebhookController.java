package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.BillingWebhookRequest;
import com.diego.odontoflowbackend.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/billing")
@RequiredArgsConstructor
@Tag(name = "Billing Webhook", description = "Payment gateway callbacks (public)")
public class BillingWebhookController {

    private final BillingService billingService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Receive a payment gateway event")
    public void receive(@Valid @RequestBody BillingWebhookRequest event) {
        billingService.handleWebhook(event);
    }
}
