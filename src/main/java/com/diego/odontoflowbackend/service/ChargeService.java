package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Charge;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.request.CreateChargeRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateChargeStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.ChargeResponse;
import com.diego.odontoflowbackend.entity.dto.response.FinancialSummaryResponse;
import com.diego.odontoflowbackend.entity.enums.ChargeStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ChargeRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChargeService {

    private final ChargeRepository chargeRepository;
    private final PatientRepository patientRepository;

    public List<ChargeResponse> list() {
        return chargeRepository.findAllByTenant(SecurityUtils.currentTenantId())
                .stream().map(ChargeResponse::from).toList();
    }

    @Transactional
    public ChargeResponse create(CreateChargeRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(request.patientId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado."));

        Charge charge = Charge.builder()
                .tenantId(tenantId)
                .patient(patient)
                .appointmentId(request.appointmentId())
                .description(request.description())
                .amount(request.amount())
                .status(ChargeStatus.PENDING)
                .dueDate(request.dueDate())
                .build();

        return ChargeResponse.from(chargeRepository.save(charge));
    }

    @Transactional
    public ChargeResponse updateStatus(UUID id, UpdateChargeStatusRequest request) {
        Charge charge = chargeRepository.findByIdAndTenantId(id, SecurityUtils.currentTenantId())
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        charge.setStatus(request.status());
        charge.setPaidAt(request.status() == ChargeStatus.PAID ? LocalDateTime.now() : null);
        return ChargeResponse.from(chargeRepository.save(charge));
    }

    public FinancialSummaryResponse summary(int year, int month) {
        UUID tenantId = SecurityUtils.currentTenantId();
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        return new FinancialSummaryResponse(
                year, month,
                chargeRepository.sumPaidInPeriod(tenantId, ChargeStatus.PAID, start, end),
                chargeRepository.sumByStatus(tenantId, ChargeStatus.PENDING)
        );
    }

    public FinancialSummaryResponse summaryForCurrentMonth() {
        LocalDate now = LocalDate.now();
        return summary(now.getYear(), now.getMonthValue());
    }
}
