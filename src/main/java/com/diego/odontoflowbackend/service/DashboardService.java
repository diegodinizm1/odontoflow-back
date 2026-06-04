package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.dto.response.AppointmentResponse;
import com.diego.odontoflowbackend.entity.dto.response.DashboardResponse;
import com.diego.odontoflowbackend.entity.dto.response.FinancialSummaryResponse;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.AppointmentRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.TenantRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TenantRepository tenantRepository;
    private final ChargeService chargeService;

    public DashboardResponse summary() {
        UUID tenantId = SecurityUtils.currentTenantId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        List<AppointmentResponse> todayAppointments = appointmentRepository
                .findInRange(tenantId, startOfDay, startOfNextDay).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELED)
                .map(AppointmentResponse::from)
                .toList();

        FinancialSummaryResponse finance = chargeService.summaryForCurrentMonth();
        long patients = patientRepository.countByTenantId(tenantId);
        long pendingRequests = appointmentRepository
                .countByTenantIdAndStatus(tenantId, AppointmentStatus.PENDING);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Clínica não encontrada."));

        return new DashboardResponse(
                patients,
                todayAppointments.size(),
                finance.paidThisMonth(),
                finance.pendingTotal(),
                pendingRequests,
                tenant.getClinicName(),
                tenant.getPublicSlug(),
                todayAppointments
        );
    }
}
