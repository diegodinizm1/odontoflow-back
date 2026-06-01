package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.UploadUrlRequest;
import com.diego.odontoflowbackend.entity.dto.response.PatientFileResponse;
import com.diego.odontoflowbackend.entity.dto.response.UploadUrlResponse;
import com.diego.odontoflowbackend.service.PatientFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/files")
@RequiredArgsConstructor
@Tag(name = "Patient Files", description = "Radiografias e exames (Pre-Signed URLs)")
@SecurityRequirement(name = "bearerAuth")
public class PatientFileController {

    private final PatientFileService fileService;

    @GetMapping
    @Operation(summary = "Listar arquivos do paciente com URLs de download temporárias")
    public List<PatientFileResponse> list(@PathVariable UUID patientId) {
        return fileService.list(patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar Pre-Signed URL para upload de arquivo")
    public UploadUrlResponse requestUpload(@PathVariable UUID patientId,
                                           @Valid @RequestBody UploadUrlRequest request) {
        return fileService.requestUpload(patientId, request);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover arquivo")
    public void delete(@PathVariable UUID patientId, @PathVariable UUID fileId) {
        fileService.delete(patientId, fileId);
    }
}
