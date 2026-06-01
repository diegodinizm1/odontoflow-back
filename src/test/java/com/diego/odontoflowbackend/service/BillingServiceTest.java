package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.billing.PaymentGateway;
import com.diego.odontoflowbackend.entity.Invoice;
import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.entity.dto.request.BillingWebhookRequest;
import com.diego.odontoflowbackend.entity.dto.response.SubscriptionResponse;
import com.diego.odontoflowbackend.entity.enums.InvoiceStatus;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.entity.enums.SubscriptionStatus;
import com.diego.odontoflowbackend.repository.InvoiceRepository;
import com.diego.odontoflowbackend.repository.SubscriptionRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentGateway paymentGateway;
    @InjectMocks BillingService service;

    private final UUID tenantId = UUID.randomUUID();
    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
    }

    @AfterEach
    void tearDown() { security.close(); }

    @Test
    void currentSubscription_createsFreeWhenMissing() {
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse res = service.currentSubscription();

        assertThat(res.plan()).isEqualTo("FREE");
        assertThat(res.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(argThat(s -> s.getPlan() == Plan.FREE));
    }

    @Test
    void subscribe_paidPlan_chargesAndActivatesWithPaidInvoice() {
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(Plan.FREE).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(eq(tenantId), any(), any())).thenReturn(new PaymentGateway.PaymentResult(true, "fake_123"));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse res = service.subscribe(Plan.PRO);

        assertThat(res.plan()).isEqualTo("PRO");
        assertThat(res.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(res.currentPeriodEnd()).isNotNull();
        verify(invoiceRepository).save(argThat(i -> i.getStatus() == InvoiceStatus.PAID
                && i.getExternalInvoiceId().equals("fake_123")));
    }

    @Test
    void subscribe_chargeFails_setsPastDueWithFailedInvoice() {
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(Plan.FREE).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(), any(), any())).thenReturn(new PaymentGateway.PaymentResult(false, "fake_fail"));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse res = service.subscribe(Plan.PRO);

        assertThat(res.status()).isEqualTo(SubscriptionStatus.PAST_DUE);
        verify(invoiceRepository).save(argThat(i -> i.getStatus() == InvoiceStatus.FAILED));
    }

    @Test
    void subscribe_freePlan_doesNotCharge() {
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(Plan.PRO).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse res = service.subscribe(Plan.FREE);

        assertThat(res.plan()).isEqualTo("FREE");
        verify(paymentGateway, never()).charge(any(), any(), any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancel_setsCanceled() {
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(Plan.PRO).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.cancel().status()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void webhook_invoiceFailed_marksPastDue() {
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(Plan.PRO).status(SubscriptionStatus.ACTIVE).build();
        Invoice inv = Invoice.builder().tenantId(tenantId).subscription(sub).amount(BigDecimal.TEN)
                .status(InvoiceStatus.PAID).externalInvoiceId("ext_1").build();
        when(invoiceRepository.findByExternalInvoiceId("ext_1")).thenReturn(Optional.of(inv));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleWebhook(new BillingWebhookRequest("invoice.failed", "ext_1"));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }
}
