package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.PatientFile;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.UploadUrlRequest;
import com.diego.odontoflowbackend.entity.dto.response.PatientFileResponse;
import com.diego.odontoflowbackend.entity.dto.response.UploadUrlResponse;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.PatientFileRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import com.diego.odontoflowbackend.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientFileService {

    private final PatientFileRepository fileRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final StorageService storage;

    @Value("${storage.presign-minutes:15}")
    private long presignMinutes;

    private Duration ttl() {
        // NFR02: pre-signed URLs valid for at most 15 minutes
        return Duration.ofMinutes(Math.min(presignMinutes, 15));
    }

    @Transactional
    public UploadUrlResponse requestUpload(UUID patientId, UploadUrlRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado."));
        User user = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        String safeName = sanitize(request.fileName());
        String key = "tenants/%s/patients/%s/%s-%s".formatted(tenantId, patientId, UUID.randomUUID(), safeName);

        PatientFile file = fileRepository.save(PatientFile.builder()
                .tenantId(tenantId)
                .patient(patient)
                .objectKey(key)
                .fileName(request.fileName())
                .contentType(request.contentType())
                .uploadedBy(user)
                .build());

        String uploadUrl = storage.presignUpload(key, request.contentType(), ttl());
        return new UploadUrlResponse(file.getId(), uploadUrl, key);
    }

    public List<PatientFileResponse> list(UUID patientId) {
        UUID tenantId = SecurityUtils.currentTenantId();
        ensurePatient(patientId, tenantId);
        return fileRepository.findByPatient(patientId, tenantId).stream()
                .map(f -> PatientFileResponse.from(f, storage.presignDownload(f.getObjectKey(), ttl())))
                .toList();
    }

    @Transactional
    public void delete(UUID patientId, UUID fileId) {
        UUID tenantId = SecurityUtils.currentTenantId();
        PatientFile file = fileRepository.findByIdAndTenantId(fileId, tenantId)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
        storage.delete(file.getObjectKey());
        fileRepository.delete(file);
    }

    private void ensurePatient(UUID patientId, UUID tenantId) {
        if (patientRepository.findByIdAndTenantId(patientId, tenantId).isEmpty()) {
            throw new NotFoundException("Paciente não encontrado.");
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
