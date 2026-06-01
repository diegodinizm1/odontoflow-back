package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Charge;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.request.CreateChargeRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateChargeStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.ChargeResponse;
import com.diego.odontoflowbackend.entity.enums.ChargeStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ChargeRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
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
class ChargeServiceTest {

    @Mock ChargeRepository chargeRepository;
    @Mock PatientRepository patientRepository;
    @InjectMocks ChargeService service;

    private final UUID tenantId  = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID chargeId  = UUID.randomUUID();

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
    }

    @AfterEach
    void tearDown() { security.close(); }

    private Patient patient() { return Patient.builder().id(patientId).tenantId(tenantId).fullName("João").build(); }

    @Test
    void create_startsPending() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChargeResponse res = service.create(new CreateChargeRequest(
                patientId, null, "Restauração", new BigDecimal("340.00"), null));

        assertThat(res.status()).isEqualTo(ChargeStatus.PENDING);
        assertThat(res.amount()).isEqualByComparingTo("340.00");
        assertThat(res.paidAt()).isNull();
    }

    @Test
    void create_patientNotFound_throws() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateChargeRequest(
                patientId, null, "x", BigDecimal.TEN, null)))
                .isInstanceOf(NotFoundException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void updateStatus_paid_setsPaidAt() {
        Charge charge = Charge.builder().id(chargeId).tenantId(tenantId).patient(patient())
                .description("x").amount(BigDecimal.TEN).status(ChargeStatus.PENDING).build();
        when(chargeRepository.findByIdAndTenantId(chargeId, tenantId)).thenReturn(Optional.of(charge));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChargeResponse res = service.updateStatus(chargeId, new UpdateChargeStatusRequest(ChargeStatus.PAID));

        assertThat(res.status()).isEqualTo(ChargeStatus.PAID);
        assertThat(res.paidAt()).isNotNull();
    }

    @Test
    void updateStatus_canceled_clearsPaidAt() {
        Charge charge = Charge.builder().id(chargeId).tenantId(tenantId).patient(patient())
                .description("x").amount(BigDecimal.TEN).status(ChargeStatus.PAID).build();
        when(chargeRepository.findByIdAndTenantId(chargeId, tenantId)).thenReturn(Optional.of(charge));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChargeResponse res = service.updateStatus(chargeId, new UpdateChargeStatusRequest(ChargeStatus.CANCELED));

        assertThat(res.status()).isEqualTo(ChargeStatus.CANCELED);
        assertThat(res.paidAt()).isNull();
    }

    @Test
    void updateStatus_notFound_throws() {
        when(chargeRepository.findByIdAndTenantId(chargeId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(chargeId, new UpdateChargeStatusRequest(ChargeStatus.PAID)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void summary_aggregatesPaidAndPending() {
        when(chargeRepository.sumPaidInPeriod(eq(tenantId), eq(ChargeStatus.PAID), any(), any()))
                .thenReturn(new BigDecimal("780.00"));
        when(chargeRepository.sumByStatus(tenantId, ChargeStatus.PENDING)).thenReturn(new BigDecimal("340.00"));

        var summary = service.summary(2026, 6);

        assertThat(summary.paidThisMonth()).isEqualByComparingTo("780.00");
        assertThat(summary.pendingTotal()).isEqualByComparingTo("340.00");
        assertThat(summary.month()).isEqualTo(6);
    }
}
