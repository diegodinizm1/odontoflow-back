package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.Tenant;
import com.diego.odontoflowbackend.entity.dto.response.DashboardResponse;
import com.diego.odontoflowbackend.entity.dto.response.FinancialSummaryResponse;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import com.diego.odontoflowbackend.repository.AppointmentRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.TenantRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock TenantRepository tenantRepository;
    @Mock ChargeService chargeService;
    @InjectMocks DashboardService service;

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
    void summary_aggregatesCountsAndExcludesCanceled() {
        Patient patient = Patient.builder().id(UUID.randomUUID()).tenantId(tenantId).fullName("João").build();
        Appointment scheduled = Appointment.builder().id(UUID.randomUUID()).tenantId(tenantId).patient(patient)
                .dentistId(UUID.randomUUID()).startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .status(AppointmentStatus.SCHEDULED).build();
        Appointment canceled = Appointment.builder().id(UUID.randomUUID()).tenantId(tenantId).patient(patient)
                .dentistId(UUID.randomUUID()).startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .status(AppointmentStatus.CANCELED).build();

        when(appointmentRepository.findInRange(eq(tenantId), any(), any())).thenReturn(List.of(scheduled, canceled));
        when(appointmentRepository.countByTenantIdAndStatus(tenantId, AppointmentStatus.PENDING)).thenReturn(3L);
        when(patientRepository.countByTenantId(tenantId)).thenReturn(7L);
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(Tenant.builder().id(tenantId).publicSlug("clinica-teste").build()));
        when(chargeService.summaryForCurrentMonth())
                .thenReturn(new FinancialSummaryResponse(2026, 6, new BigDecimal("780.00"), new BigDecimal("340.00")));

        DashboardResponse res = service.summary();

        assertThat(res.patientsCount()).isEqualTo(7L);
        assertThat(res.appointmentsToday()).isEqualTo(1L); // canceled excluded
        assertThat(res.todayAppointments()).hasSize(1);
        assertThat(res.paidThisMonth()).isEqualByComparingTo("780.00");
        assertThat(res.pendingTotal()).isEqualByComparingTo("340.00");
        assertThat(res.pendingBookingRequests()).isEqualTo(3L);
        assertThat(res.publicSlug()).isEqualTo("clinica-teste");
    }
}
