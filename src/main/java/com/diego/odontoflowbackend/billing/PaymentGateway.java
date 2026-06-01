package com.diego.odontoflowbackend.billing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstraction over a payment provider (Stripe, Mercado Pago, …).
 * A {@code FakePaymentGateway} is used for local/dev; a real provider
 * implementation can be swapped in for production — same pattern as the
 * S3/MinIO StorageService.
 */
public interface PaymentGateway {

    PaymentResult charge(UUID tenantId, String description, BigDecimal amount);

    record PaymentResult(boolean success, String externalId) {}
}
