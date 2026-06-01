package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.TestcontainersConfiguration;
import com.diego.odontoflowbackend.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PatientFileControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean StorageService storage;

    private String token;
    private String patientId;

    @BeforeEach
    void stubStorage() {
        // @MockitoBean is reset between tests, so (re)stub before each
        when(storage.presignUpload(anyString(), anyString(), any())).thenReturn("http://minio.local/upload");
        when(storage.presignDownload(anyString(), any())).thenReturn("http://minio.local/download");
    }

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Files IT",
                                "document", "88.888.888/0001-88",
                                "fullName", "Dra. Files",
                                "email", "files@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "files@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();

        String patientBody = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Paciente Files"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        patientId = objectMapper.readTree(patientBody).get("id").asText();
    }

    private String requestUpload() throws Exception {
        return mockMvc.perform(post("/patients/" + patientId + "/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fileName", "panoramica.png", "contentType", "image/png"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uploadUrl").value("http://minio.local/upload"))
                .andExpect(jsonPath("$.fileId").isNotEmpty())
                .andExpect(jsonPath("$.key").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void requestUpload_returns201WithPresignedUrl() throws Exception {
        requestUpload();
    }

    @Test
    void list_returnsFilesWithDownloadUrl() throws Exception {
        requestUpload();

        mockMvc.perform(get("/patients/" + patientId + "/files")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].downloadUrl").value("http://minio.local/download"))
                .andExpect(jsonPath("$[0].fileName").value("panoramica.png"));
    }

    @Test
    void delete_returns204() throws Exception {
        String body = requestUpload();
        String fileId = objectMapper.readTree(body).get("fileId").asText();

        mockMvc.perform(delete("/patients/" + patientId + "/files/" + fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void requestUpload_unknownPatient_returns404() throws Exception {
        mockMvc.perform(post("/patients/00000000-0000-0000-0000-000000000000/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fileName", "x.png", "contentType", "image/png"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void accessWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/patients/" + patientId + "/files"))
                .andExpect(status().isForbidden());
    }
}
