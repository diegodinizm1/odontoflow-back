package com.diego.odontoflowbackend.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Real Stripe gateway (Stripe Java SDK). Activated with {@code billing.provider=stripe}.
 *
 * The API base can be pointed at {@code stripe-mock} (offline/CI) or real Stripe
 * test mode via {@code stripe.base-url} / {@code stripe.api-key}. Note: stripe-mock
 * returns canned fixtures and does not emit webhooks — the paid/failed lifecycle is
 * driven by {@code /webhooks/billing} (or the Stripe CLI against real test mode).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "billing.provider", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    public StripePaymentGateway(@Value("${stripe.api-key}") String apiKey,
                                @Value("${stripe.base-url:}") String baseUrl) {
        Stripe.apiKey = apiKey;
        if (StringUtils.hasText(baseUrl)) {
            Stripe.overrideApiBase(baseUrl);
        }
        log.info("StripePaymentGateway active (base = {})", StringUtils.hasText(baseUrl) ? baseUrl : "https://api.stripe.com");
    }

    @Override
    public PaymentResult charge(UUID tenantId, String description, BigDecimal amount) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValueExact()) // cents
                    .setCurrency("brl")
                    .setDescription(description)
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return new PaymentResult(true, intent.getId());
        } catch (StripeException e) {
            log.error("[StripePaymentGateway] charge failed for tenant {}: {}", tenantId, e.getMessage());
            return new PaymentResult(false, null);
        }
    }
}
