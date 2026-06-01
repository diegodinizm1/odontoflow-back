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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalRecordService {

    private final ClinicalRecordRepository recordRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public List<ClinicalRecordResponse> listByPatient(UUID patientId) {
        ensurePatient(patientId);
        return recordRepository.findHistory(patientId, SecurityUtils.currentTenantId())
                .stream().map(ClinicalRecordResponse::from).toList();
    }

    /** Estado mais recente do odontograma do paciente (ou vazio). */
    public Map<String, ToothState> latestOdontogram(UUID patientId) {
        ensurePatient(patientId);
        return recordRepository.findHistory(patientId, SecurityUtils.currentTenantId())
                .stream().findFirst()
                .map(ClinicalRecord::getOdontogramData)
                .orElseGet(HashMap::new);
    }

    @Transactional
    public ClinicalRecordResponse create(UUID patientId, CreateClinicalRecordRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado."));

        User dentist = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        ClinicalRecord record = ClinicalRecord.builder()
                .tenantId(tenantId)
                .patient(patient)
                .appointmentId(request.appointmentId())
                .odontogramData(request.odontogramData() != null ? request.odontogramData() : new HashMap<>())
                .clinicalNotes(request.clinicalNotes())
                .createdBy(dentist)
                .build();

        return ClinicalRecordResponse.from(recordRepository.save(record));
    }

    private void ensurePatient(UUID patientId) {
        if (patientRepository.findByIdAndTenantId(patientId, SecurityUtils.currentTenantId()).isEmpty()) {
            throw new NotFoundException("Paciente não encontrado.");
        }
    }
}
