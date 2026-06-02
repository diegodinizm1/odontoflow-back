package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.request.CreatePatientRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdatePatientRequest;
import com.diego.odontoflowbackend.entity.dto.response.PatientResponse;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.exception.PlanLimitExceededException;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.SubscriptionRepository;
import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final SubscriptionRepository subscriptionRepository;

    public List<PatientResponse> listAll() {
        return patientRepository.findAllByTenantId(SecurityUtils.currentTenantId())
                .stream().map(PatientResponse::from).toList();
    }

    public PatientResponse findById(UUID id) {
        return PatientResponse.from(getOrThrow(id));
    }

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        enforcePatientLimit(tenantId);

        Patient patient = Patient.builder()
                .tenantId(tenantId)
                .fullName(request.fullName())
                .phone(request.phone())
                .dateOfBirth(request.dateOfBirth())
                .medicalAlerts(request.medicalAlerts())
                .build();
        return PatientResponse.from(patientRepository.save(patient));
    }

    private void enforcePatientLimit(UUID tenantId) {
        Plan plan = subscriptionRepository.findByTenantId(tenantId)
                .map(Subscription::getPlan).orElse(Plan.FREE);
        long count = patientRepository.countByTenantId(tenantId);
        if (count >= plan.getMaxPatients()) {
            throw new PlanLimitExceededException(
                    "Limite de %d pacientes do plano %s atingido. Faça upgrade do plano."
                            .formatted(plan.getMaxPatients(), plan.getDisplayName()));
        }
    }

    @Transactional
    public PatientResponse update(UUID id, UpdatePatientRequest request) {
        Patient patient = getOrThrow(id);
        patient.setFullName(request.fullName());
        patient.setPhone(request.phone());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setMedicalAlerts(request.medicalAlerts());
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public void delete(UUID id) {
        if (!patientRepository.existsByIdAndTenantId(id, SecurityUtils.currentTenantId())) {
            throw new NotFoundException("Paciente não encontrado.");
        }
        patientRepository.deleteById(id);
    }

    private Patient getOrThrow(UUID id) {
        return patientRepository.findByIdAndTenantId(id, SecurityUtils.currentTenantId())
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado."));
    }
}
