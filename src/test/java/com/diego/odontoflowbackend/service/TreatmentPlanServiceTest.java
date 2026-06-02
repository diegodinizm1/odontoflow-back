package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Charge;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.TreatmentItem;
import com.diego.odontoflowbackend.entity.TreatmentPlan;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.CreateTreatmentPlanRequest;
import com.diego.odontoflowbackend.entity.dto.response.TreatmentPlanResponse;
import com.diego.odontoflowbackend.entity.enums.TreatmentItemStatus;
import com.diego.odontoflowbackend.entity.enums.TreatmentPlanStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ChargeRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.TreatmentPlanRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    @Mock TreatmentPlanRepository planRepository;
    @Mock PatientRepository patientRepository;
    @Mock UserRepository userRepository;
    @Mock ChargeRepository chargeRepository;
    @InjectMocks TreatmentPlanService service;

    private final UUID tenantId  = UUID.randomUUID();
    private final UUID userId    = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID planId    = UUID.randomUUID();

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
        security.when(SecurityUtils::currentUserId).thenReturn(userId);
    }

    @AfterEach
    void tearDown() { security.close(); }

    private Patient patient() { return Patient.builder().id(patientId).tenantId(tenantId).fullName("João").build(); }
    private User dentist() { return User.builder().id(userId).tenantId(tenantId).fullName("Dra. Ana").build(); }

    @Test
    void create_buildsProposedPlanWithItemsAndTotal() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(dentist()));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new CreateTreatmentPlanRequest("Reabilitação", List.of(
                new CreateTreatmentPlanRequest.Item("Canal", "36", new BigDecimal("1250.00")),
                new CreateTreatmentPlanRequest.Item("Restauração", "26", new BigDecimal("340.00"))));

        TreatmentPlanResponse res = service.create(patientId, req);

        assertThat(res.status()).isEqualTo(TreatmentPlanStatus.PROPOSED);
        assertThat(res.items()).hasSize(2);
        assertThat(res.total()).isEqualByComparingTo("1590.00");
        assertThat(res.createdByName()).isEqualTo("Dra. Ana");
    }

    @Test
    void create_patientNotFound_throws() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(patientId,
                new CreateTreatmentPlanRequest("X", List.of(
                        new CreateTreatmentPlanRequest.Item("a", null, BigDecimal.TEN)))))
                .isInstanceOf(NotFoundException.class);
        verify(planRepository, never()).save(any());
    }

    @Test
    void completeItem_marksDoneAndGeneratesCharge() {
        UUID itemId = UUID.randomUUID();
        TreatmentItem item = TreatmentItem.builder().id(itemId).description("Canal").tooth("36")
                .amount(new BigDecimal("1250.00")).status(TreatmentItemStatus.PENDING).build();
        TreatmentPlan plan = TreatmentPlan.builder().id(planId).tenantId(tenantId).patient(patient())
                .title("P").status(TreatmentPlanStatus.PROPOSED).createdBy(dentist()).build();
        plan.addItem(item);

        when(planRepository.findByIdAndTenantId(planId, tenantId)).thenReturn(Optional.of(plan));
        UUID chargeId = UUID.randomUUID();
        when(chargeRepository.save(any())).thenAnswer(inv -> { Charge c = inv.getArgument(0); c.setId(chargeId); return c; });
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TreatmentPlanResponse res = service.completeItem(planId, itemId);

        verify(chargeRepository).save(argThat(c -> c.getAmount().compareTo(new BigDecimal("1250.00")) == 0
                && c.getPatient().getId().equals(patientId)));
        assertThat(res.items().get(0).status()).isEqualTo(TreatmentItemStatus.DONE);
        assertThat(res.items().get(0).chargeId()).isEqualTo(chargeId);
        // single item -> plan fully completed
        assertThat(res.status()).isEqualTo(TreatmentPlanStatus.COMPLETED);
    }

    @Test
    void completeItem_isIdempotent_noDuplicateCharge() {
        UUID itemId = UUID.randomUUID();
        TreatmentItem item = TreatmentItem.builder().id(itemId).description("Canal")
                .amount(BigDecimal.TEN).status(TreatmentItemStatus.DONE).chargeId(UUID.randomUUID()).build();
        TreatmentPlan plan = TreatmentPlan.builder().id(planId).tenantId(tenantId).patient(patient())
                .title("P").status(TreatmentPlanStatus.COMPLETED).createdBy(dentist()).build();
        plan.addItem(item);
        when(planRepository.findByIdAndTenantId(planId, tenantId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeItem(planId, itemId);

        verify(chargeRepository, never()).save(any());
    }

    @Test
    void completeItem_planNotFound_throws() {
        when(planRepository.findByIdAndTenantId(planId, tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.completeItem(planId, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
