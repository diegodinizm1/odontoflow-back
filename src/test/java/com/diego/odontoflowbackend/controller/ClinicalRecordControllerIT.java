package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClinicalRecordControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String patientId;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Prontuário IT",
                                "document", "77.777.777/0001-77",
                                "fullName", "Dra. Prontuário",
                                "email", "prontuario@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "prontuario@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();

        String patientBody = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Paciente Prontuário"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        patientId = objectMapper.readTree(patientBody).get("id").asText();
    }

    private String recordBody(String condition, String note) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "odontogramData", Map.of("18", Map.of("condition", condition, "surfaces", java.util.List.of("O"))),
                "clinicalNotes", note));
    }

    @Test
    void create_asDentist_returns201WithSignature() throws Exception {
        mockMvc.perform(post("/patients/" + patientId + "/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody("CARIES", "Cárie identificada no dente 18")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdByName").value("Dra. Prontuário"))
                .andExpect(jsonPath("$.odontogramData.18.condition").value("CARIES"))
                .andExpect(jsonPath("$.odontogramData.18.surfaces[0]").value("O"));
    }

    @Test
    void list_returnsHistory() throws Exception {
        mockMvc.perform(post("/patients/" + patientId + "/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody("RESTORED", "Restauração concluída")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/patients/" + patientId + "/records")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].createdByName").value("Dra. Prontuário"));
    }

    @Test
    void latestOdontogram_reflectsLastSave() throws Exception {
        mockMvc.perform(post("/patients/" + patientId + "/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody("CROWN", "Coroa instalada")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/patients/" + patientId + "/odontogram")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.18.condition").value("CROWN"));
    }

    @Test
    void create_unknownPatient_returns404() throws Exception {
        mockMvc.perform(post("/patients/00000000-0000-0000-0000-000000000000/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody("CARIES", "x")))
                .andExpect(status().isNotFound());
    }

    @Test
    void accessWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/patients/" + patientId + "/records"))
                .andExpect(status().isForbidden());
    }
}
