package com.diego.odontoflowbackend.billing;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Stripe client plumbing against {@code stripe-mock} (offline, no account).
 * stripe-mock returns canned fixtures, so this proves request/response wiring — not the
 * paid/failed lifecycle, which is webhook-driven.
 */
@Testcontainers
class StripePaymentGatewayIT {

    @Container
    static final GenericContainer<?> stripeMock =
            new GenericContainer<>(DockerImageName.parse("stripe/stripe-mock:latest"))
                    .withExposedPorts(12111);

    @Test
    void charge_createsPaymentIntentViaStripeMock() {
        String baseUrl = "http://" + stripeMock.getHost() + ":" + stripeMock.getMappedPort(12111);
        StripePaymentGateway gateway = new StripePaymentGateway("sk_test_123", baseUrl);

        PaymentGateway.PaymentResult result =
                gateway.charge(UUID.randomUUID(), "Assinatura Pro", new BigDecimal("149.90"));

        assertThat(result.success()).isTrue();
        assertThat(result.externalId()).isNotBlank();
    }
}
