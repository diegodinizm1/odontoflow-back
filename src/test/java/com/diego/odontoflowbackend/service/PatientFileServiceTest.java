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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientFileServiceTest {

    @Mock PatientFileRepository fileRepository;
    @Mock PatientRepository patientRepository;
    @Mock UserRepository userRepository;
    @Mock StorageService storage;
    @InjectMocks PatientFileService service;

    private final UUID tenantId  = UUID.randomUUID();
    private final UUID userId    = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID fileId    = UUID.randomUUID();

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "presignMinutes", 15L);
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
        security.when(SecurityUtils::currentUserId).thenReturn(userId);
    }

    @AfterEach
    void tearDown() { security.close(); }

    private Patient patient() { return Patient.builder().id(patientId).tenantId(tenantId).fullName("João").build(); }
    private User user() { return User.builder().id(userId).tenantId(tenantId).fullName("Dra. Ana").build(); }

    @Test
    void requestUpload_persistsAndReturnsPresignedUrl() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user()));
        when(fileRepository.save(any())).thenAnswer(inv -> { PatientFile f = inv.getArgument(0); f.setId(fileId); return f; });
        when(storage.presignUpload(anyString(), eq("image/png"), any())).thenReturn("http://minio/put");

        UploadUrlResponse res = service.requestUpload(patientId, new UploadUrlRequest("raio-x.png", "image/png"));

        assertThat(res.uploadUrl()).isEqualTo("http://minio/put");
        assertThat(res.fileId()).isEqualTo(fileId);
        assertThat(res.key()).contains("tenants/" + tenantId).contains("patients/" + patientId);
        verify(storage).presignUpload(anyString(), eq("image/png"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void requestUpload_patientNotFound_throws() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestUpload(patientId, new UploadUrlRequest("a.png", "image/png")))
                .isInstanceOf(NotFoundException.class);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void list_returnsFilesWithDownloadUrl() {
        when(patientRepository.findByIdAndTenantId(patientId, tenantId)).thenReturn(Optional.of(patient()));
        PatientFile f = PatientFile.builder().id(fileId).tenantId(tenantId).patient(patient())
                .objectKey("k").fileName("raio-x.png").contentType("image/png").uploadedBy(user()).build();
        when(fileRepository.findByPatient(patientId, tenantId)).thenReturn(List.of(f));
        when(storage.presignDownload("k", Duration.ofMinutes(15))).thenReturn("http://minio/get");

        List<PatientFileResponse> res = service.list(patientId);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).downloadUrl()).isEqualTo("http://minio/get");
        assertThat(res.get(0).uploadedByName()).isEqualTo("Dra. Ana");
    }

    @Test
    void delete_removesObjectAndRow() {
        PatientFile f = PatientFile.builder().id(fileId).tenantId(tenantId).objectKey("k").build();
        when(fileRepository.findByIdAndTenantId(fileId, tenantId)).thenReturn(Optional.of(f));

        service.delete(patientId, fileId);

        verify(storage).delete("k");
        verify(fileRepository).delete(f);
    }

    @Test
    void delete_notFound_throws() {
        when(fileRepository.findByIdAndTenantId(fileId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(patientId, fileId)).isInstanceOf(NotFoundException.class);
        verify(storage, never()).delete(anyString());
    }
}
