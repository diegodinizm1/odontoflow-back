package com.diego.odontoflowbackend.billing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Local/dev payment gateway that instantly approves charges — analogous to
 * using MinIO in place of real S3. Returns a synthetic external id so the
 * rest of the billing flow (invoices, webhooks) behaves like production.
 */
@Service
@Slf4j
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(UUID tenantId, String description, BigDecimal amount) {
        String externalId = "fake_inv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[FakePaymentGateway] charged tenant {} amount {} -> {}", tenantId, amount, externalId);
        return new PaymentResult(true, externalId);
    }
}
