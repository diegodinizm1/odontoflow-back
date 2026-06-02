package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Charge;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.TreatmentItem;
import com.diego.odontoflowbackend.entity.TreatmentPlan;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.CreateTreatmentPlanRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateTreatmentPlanStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.TreatmentPlanResponse;
import com.diego.odontoflowbackend.entity.enums.ChargeStatus;
import com.diego.odontoflowbackend.entity.enums.TreatmentItemStatus;
import com.diego.odontoflowbackend.entity.enums.TreatmentPlanStatus;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ChargeRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.TreatmentPlanRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TreatmentPlanService {

    private final TreatmentPlanRepository planRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ChargeRepository chargeRepository;

    public List<TreatmentPlanResponse> listByPatient(UUID patientId) {
        ensurePatient(patientId);
        return planRepository.findByPatient(patientId, SecurityUtils.currentTenantId())
                .stream().map(TreatmentPlanResponse::from).toList();
    }

    @Transactional
    public TreatmentPlanResponse create(UUID patientId, CreateTreatmentPlanRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado."));
        User dentist = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        TreatmentPlan plan = TreatmentPlan.builder()
                .tenantId(tenantId)
                .patient(patient)
                .title(request.title())
                .status(TreatmentPlanStatus.PROPOSED)
                .createdBy(dentist)
                .build();

        request.items().forEach(i -> plan.addItem(TreatmentItem.builder()
                .description(i.description())
                .tooth(i.tooth())
                .amount(i.amount())
                .status(TreatmentItemStatus.PENDING)
                .build()));

        return TreatmentPlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public TreatmentPlanResponse updateStatus(UUID planId, UpdateTreatmentPlanStatusRequest request) {
        TreatmentPlan plan = getOrThrow(planId);
        plan.setStatus(request.status());
        return TreatmentPlanResponse.from(planRepository.save(plan));
    }

    /** Marks an item as done and generates a pending Charge for it (idempotent). */
    @Transactional
    public TreatmentPlanResponse completeItem(UUID planId, UUID itemId) {
        TreatmentPlan plan = getOrThrow(planId);
        TreatmentItem item = plan.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Item do plano não encontrado."));

        if (item.getStatus() != TreatmentItemStatus.DONE) {
            item.setStatus(TreatmentItemStatus.DONE);

            Charge charge = chargeRepository.save(Charge.builder()
                    .tenantId(plan.getTenantId())
                    .patient(plan.getPatient())
                    .description(item.getDescription() + (item.getTooth() != null ? " (dente " + item.getTooth() + ")" : ""))
                    .amount(item.getAmount())
                    .status(ChargeStatus.PENDING)
                    .build());
            item.setChargeId(charge.getId());

            if (plan.getStatus() == TreatmentPlanStatus.PROPOSED) {
                plan.setStatus(TreatmentPlanStatus.ACCEPTED);
            }
            if (plan.getItems().stream().allMatch(i -> i.getStatus() == TreatmentItemStatus.DONE)) {
                plan.setStatus(TreatmentPlanStatus.COMPLETED);
            }
        }

        return TreatmentPlanResponse.from(planRepository.save(plan));
    }

    private TreatmentPlan getOrThrow(UUID planId) {
        return planRepository.findByIdAndTenantId(planId, SecurityUtils.currentTenantId())
                .orElseThrow(() -> new NotFoundException("Plano de tratamento não encontrado."));
    }

    private void ensurePatient(UUID patientId) {
        if (patientRepository.findByIdAndTenantId(patientId, SecurityUtils.currentTenantId()).isEmpty()) {
            throw new NotFoundException("Paciente não encontrado.");
        }
    }
}
