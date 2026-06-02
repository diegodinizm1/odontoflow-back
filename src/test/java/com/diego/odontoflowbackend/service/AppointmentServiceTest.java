package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.request.CreateAppointmentRequest;
import com.diego.odontoflowbackend.entity.dto.request.RescheduleAppointmentRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateAppointmentStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.AppointmentResponse;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import com.diego.odontoflowbackend.exception.BadRequestException;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.AppointmentRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @InjectMocks AppointmentService appointmentService;

    private final UUID tenantId    = UUID.randomUUID();
    private final UUID userId      = UUID.randomUUID();
    private final UUID patientId   = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    private final LocalDateTime start = LocalDateTime.of(2026, 6, 2, 9, 0);
    private final LocalDateTime end   = LocalDateTime.of(2026, 6, 2, 10, 0);

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

    private Appointment appointment() {
        return Appointment.builder()
                .id(appointmentId).tenantId(tenantId).patient(patient()).dentistId(userId)
                .startTime(start).endTime(end).status(AppointmentStatus.SCHEDULED).build();
    }

    @Test
    void create_success_defaultsDentistToCurrentUser() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(appointmentRepository.existsOverlap(tenantId, userId, start, end, null)).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse res = appointmentService.create(
                new CreateAppointmentRequest(patientId, null, start, end));

        assertThat(res.dentistId()).isEqualTo(userId);
        assertThat(res.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(res.patientName()).isEqualTo("João Silva");
    }

    @Test
    void create_overlap_throwsConflict() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(appointmentRepository.existsOverlap(tenantId, userId, start, end, null)).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(patientId, null, start, end)))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void create_endBeforeStart_throwsBadRequest() {
        assertThatThrownBy(() -> appointmentService.create(
                new CreateAppointmentRequest(patientId, null, end, start)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_patientNotFound_throwsNotFound() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(patientId, null, start, end)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listInRange_endBeforeStart_throwsBadRequest() {
        assertThatThrownBy(() -> appointmentService.listInRange(end, start, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listInRange_dentist_isScopedToOwnAgenda() {
        security.when(SecurityUtils::currentRole).thenReturn("DENTIST");
        when(appointmentRepository.findInRangeByDentist(tenantId, userId, start, end)).thenReturn(List.of());

        appointmentService.listInRange(start, end, UUID.randomUUID()); // requested filter is ignored

        verify(appointmentRepository).findInRangeByDentist(tenantId, userId, start, end);
        verify(appointmentRepository, never()).findInRange(any(), any(), any());
    }

    @Test
    void listInRange_receptionist_seesAllWhenNoFilter() {
        security.when(SecurityUtils::currentRole).thenReturn("RECEPTIONIST");
        when(appointmentRepository.findInRange(tenantId, start, end)).thenReturn(List.of());

        appointmentService.listInRange(start, end, null);

        verify(appointmentRepository).findInRange(tenantId, start, end);
        verify(appointmentRepository, never()).findInRangeByDentist(any(), any(), any(), any());
    }

    @Test
    void listInRange_receptionist_filtersByDentist() {
        UUID otherDentist = UUID.randomUUID();
        security.when(SecurityUtils::currentRole).thenReturn("RECEPTIONIST");
        when(appointmentRepository.findInRangeByDentist(tenantId, otherDentist, start, end)).thenReturn(List.of());

        appointmentService.listInRange(start, end, otherDentist);

        verify(appointmentRepository).findInRangeByDentist(tenantId, otherDentist, start, end);
    }

    @Test
    void reschedule_excludesSelfFromOverlapCheck() {
        when(appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)).thenReturn(Optional.of(appointment()));
        when(appointmentRepository.existsOverlap(eq(tenantId), eq(userId), any(), any(), eq(appointmentId))).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime newStart = start.plusHours(2);
        LocalDateTime newEnd = end.plusHours(2);
        AppointmentResponse res = appointmentService.reschedule(appointmentId,
                new RescheduleAppointmentRequest(newStart, newEnd));

        assertThat(res.startTime()).isEqualTo(newStart);
        verify(appointmentRepository).existsOverlap(tenantId, userId, newStart, newEnd, appointmentId);
    }

    @Test
    void reschedule_canceledAppointment_throwsBadRequest() {
        Appointment canceled = appointment();
        canceled.setStatus(AppointmentStatus.CANCELED);
        when(appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)).thenReturn(Optional.of(canceled));

        assertThatThrownBy(() -> appointmentService.reschedule(appointmentId,
                new RescheduleAppointmentRequest(start, end)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_setsNewStatus() {
        when(appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)).thenReturn(Optional.of(appointment()));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse res = appointmentService.updateStatus(appointmentId,
                new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED));

        assertThat(res.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }
}
