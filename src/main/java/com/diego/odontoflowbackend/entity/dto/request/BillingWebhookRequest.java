package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Simulated gateway webhook payload (e.g. type = "invoice.paid" | "invoice.failed"). */
public record BillingWebhookRequest(
        @NotBlank String type,
        @NotBlank String externalInvoiceId
) {}
