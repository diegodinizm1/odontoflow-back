package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.billing.PaymentGateway;
import com.diego.odontoflowbackend.entity.Invoice;
import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.entity.dto.request.BillingWebhookRequest;
import com.diego.odontoflowbackend.entity.dto.response.InvoiceResponse;
import com.diego.odontoflowbackend.entity.dto.response.PlanResponse;
import com.diego.odontoflowbackend.entity.dto.response.SubscriptionResponse;
import com.diego.odontoflowbackend.entity.enums.InvoiceStatus;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.entity.enums.SubscriptionStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.InvoiceRepository;
import com.diego.odontoflowbackend.repository.SubscriptionRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;

    public List<PlanResponse> plans() {
        return Arrays.stream(Plan.values()).map(PlanResponse::from).toList();
    }

    @Transactional
    public SubscriptionResponse currentSubscription() {
        return SubscriptionResponse.from(getOrCreate(SecurityUtils.currentTenantId()));
    }

    @Transactional
    public SubscriptionResponse subscribe(Plan plan) {
        UUID tenantId = SecurityUtils.currentTenantId();
        Subscription subscription = getOrCreate(tenantId);

        if (!plan.isPaid()) {
            subscription.setPlan(plan);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodEnd(null);
            return SubscriptionResponse.from(subscriptionRepository.save(subscription));
        }

        PaymentGateway.PaymentResult result = paymentGateway.charge(
                tenantId, "Assinatura " + plan.getDisplayName(), plan.getMonthlyPrice());

        Invoice invoice = Invoice.builder()
                .tenantId(tenantId)
                .subscription(subscription)
                .description("Assinatura " + plan.getDisplayName())
                .amount(plan.getMonthlyPrice())
                .dueDate(LocalDate.now())
                .externalInvoiceId(result.externalId())
                .status(result.success() ? InvoiceStatus.PAID : InvoiceStatus.FAILED)
                .paidAt(result.success() ? LocalDateTime.now() : null)
                .build();
        invoiceRepository.save(invoice);

        if (result.success()) {
            subscription.setPlan(plan);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodEnd(LocalDate.now().plusMonths(1));
        } else {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
        }
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse cancel() {
        Subscription subscription = getOrCreate(SecurityUtils.currentTenantId());
        subscription.setStatus(SubscriptionStatus.CANCELED);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    public List<InvoiceResponse> invoices() {
        return invoiceRepository.findByTenantIdOrderByCreatedAtDesc(SecurityUtils.currentTenantId())
                .stream().map(InvoiceResponse::from).toList();
    }

    /** Handles a (simulated) gateway webhook — no tenant context, keyed by external invoice id. */
    @Transactional
    public void handleWebhook(BillingWebhookRequest event) {
        Invoice invoice = invoiceRepository.findByExternalInvoiceId(event.externalInvoiceId())
                .orElseThrow(() -> new NotFoundException("Fatura não encontrada."));
        Subscription subscription = invoice.getSubscription();

        switch (event.type()) {
            case "invoice.paid" -> {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidAt(LocalDateTime.now());
                subscription.setStatus(SubscriptionStatus.ACTIVE);
            }
            case "invoice.failed" -> {
                invoice.setStatus(InvoiceStatus.FAILED);
                subscription.setStatus(SubscriptionStatus.PAST_DUE);
            }
            default -> { /* ignore unknown event types */ }
        }
        invoiceRepository.save(invoice);
        subscriptionRepository.save(subscription);
    }

    private Subscription getOrCreate(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> subscriptionRepository.save(Subscription.builder()
                        .tenantId(tenantId)
                        .plan(Plan.FREE)
                        .status(SubscriptionStatus.ACTIVE)
                        .build()));
    }
}
