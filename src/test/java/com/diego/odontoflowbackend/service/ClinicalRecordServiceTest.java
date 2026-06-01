package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.ClinicalRecord;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.ToothState;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.CreateClinicalRecordRequest;
import com.diego.odontoflowbackend.entity.dto.response.ClinicalRecordResponse;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ClinicalRecordRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordServiceTest {

    @Mock ClinicalRecordRepository recordRepository;
    @Mock PatientRepository patientRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ClinicalRecordService service;

    private final UUID tenantId  = UUID.randomUUID();
    private final UUID userId    = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
        security.when(SecurityUtils::currentUserId).thenReturn(userId);
    }

    @AfterEach
    void tearDown() { security.close(); }

    private Patient patient() {
        return Patient.builder().id(patientId).tenantId(tenantId).fullName("João Silva").build();
    }

    private User dentist() {
        return User.builder().id(userId).tenantId(tenantId).fullName("Dra. Ana").build();
    }

    @Test
    void create_savesWithCurrentUserAndPatient() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(dentist()));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var odontogram = Map.of("18", new ToothState("CARIES", List.of("O")));
        ClinicalRecordResponse res = service.create(patientId,
                new CreateClinicalRecordRequest(odontogram, "Restauração em 18", null));

        assertThat(res.createdByName()).isEqualTo("Dra. Ana");
        assertThat(res.odontogramData()).containsKey("18");
        assertThat(res.clinicalNotes()).isEqualTo("Restauração em 18");
        verify(recordRepository).save(argThat(r -> r.getTenantId().equals(tenantId) && r.getCreatedBy() != null));
    }

    @Test
    void create_nullOdontogram_defaultsToEmpty() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(dentist()));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClinicalRecordResponse res = service.create(patientId,
                new CreateClinicalRecordRequest(null, "Apenas nota", null));

        assertThat(res.odontogramData()).isEmpty();
    }

    @Test
    void create_patientNotFound_throwsNotFound() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(patientId, new CreateClinicalRecordRequest(Map.of(), "x", null)))
                .isInstanceOf(NotFoundException.class);
        verify(recordRepository, never()).save(any());
    }

    @Test
    void latestOdontogram_empty_whenNoRecords() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(recordRepository.findHistory(patientId, tenantId)).thenReturn(List.of());

        assertThat(service.latestOdontogram(patientId)).isEmpty();
    }

    @Test
    void latestOdontogram_returnsMostRecent() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        ClinicalRecord latest = ClinicalRecord.builder()
                .odontogramData(Map.of("11", new ToothState("RESTORED", List.of("V")))).build();
        when(recordRepository.findHistory(patientId, tenantId)).thenReturn(List.of(latest));

        assertThat(service.latestOdontogram(patientId)).containsKey("11");
    }

    @Test
    void listByPatient_patientNotFound_throwsNotFound() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByPatient(patientId))
                .isInstanceOf(NotFoundException.class);
    }
}
