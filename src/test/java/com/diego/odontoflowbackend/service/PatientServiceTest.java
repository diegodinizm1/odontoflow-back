package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.request.CreatePatientRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdatePatientRequest;
import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.entity.dto.response.PatientResponse;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.entity.enums.SubscriptionStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.exception.PlanLimitExceededException;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.diego.odontoflowbackend.security.SecurityUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @InjectMocks PatientService patientService;

    private final UUID tenantId  = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    private Patient patient() {
        return Patient.builder()
                .id(patientId).tenantId(tenantId)
                .fullName("João Silva").dateOfBirth(LocalDate.of(1990, 5, 20))
                .medicalAlerts("Alérgico a penicilina").build();
    }

    @Test
    void listAll_returnsOnlyTenantPatients() {
        when(patientRepository.findAllByTenantId(tenantId)).thenReturn(List.of(patient()));

        List<PatientResponse> result = patientService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("João Silva");
    }

    @Test
    void findById_existingPatient_returnsResponse() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));

        PatientResponse response = patientService.findById(patientId);

        assertThat(response.id()).isEqualTo(patientId);
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        when(patientRepository.findByIdAndTenantId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findById(patientId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_savesWithTenantId() {
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.empty()); // -> FREE plan (50)
        when(patientRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = patientService.create(
                new CreatePatientRequest("Maria Souza", "(11) 99999-8888", LocalDate.of(1985, 3, 10), null));

        assertThat(response.fullName()).isEqualTo("Maria Souza");
        assertThat(response.phone()).isEqualTo("(11) 99999-8888");
        verify(patientRepository).save(argThat(p -> p.getTenantId().equals(tenantId)));
    }

    @Test
    void create_atPlanLimit_throwsPlanLimitExceeded() {
        Subscription free = Subscription.builder().tenantId(tenantId)
                .plan(Plan.FREE).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(free));
        when(patientRepository.countByTenantId(tenantId)).thenReturn(50L); // FREE limit reached

        assertThatThrownBy(() -> patientService.create(
                new CreatePatientRequest("Excedente", null, null, null)))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("upgrade");
        verify(patientRepository, never()).save(any());
    }

    @Test
    void update_existingPatient_updatesFields() {
        Patient p = patient();
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(p));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = patientService.update(patientId,
                new UpdatePatientRequest("João Atualizado", "(11) 98888-7777", LocalDate.of(1990, 5, 20), "Sem alergias"));

        assertThat(response.fullName()).isEqualTo("João Atualizado");
        assertThat(response.phone()).isEqualTo("(11) 98888-7777");
        assertThat(response.medicalAlerts()).isEqualTo("Sem alergias");
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        when(patientRepository.existsByIdAndTenantId(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> patientService.delete(patientId))
                .isInstanceOf(NotFoundException.class);
    }
}
