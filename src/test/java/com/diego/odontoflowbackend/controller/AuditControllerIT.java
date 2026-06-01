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
class AuditControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Audit IT",
                                "document", "10.101.010/0001-10",
                                "fullName", "Dra. Audit",
                                "email", "audit@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "audit@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();
    }

    @Test
    void patientChanges_areAudited_withUserAndRevisionTypes() throws Exception {
        // create
        String created = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Antes Silva"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        // update -> second revision
        mockMvc.perform(put("/patients/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Depois Silva", "medicalAlerts", "Alérgico a látex"))))
                .andExpect(status().isOk());

        // audit history: 2 revisions, newest first (MOD then ADD), user id stamped
        mockMvc.perform(get("/patients/" + id + "/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].revisionType").value("MOD"))
                .andExpect(jsonPath("$[0].fullName").value("Depois Silva"))
                .andExpect(jsonPath("$[0].medicalAlerts").value("Alérgico a látex"))
                .andExpect(jsonPath("$[0].changedByUserId").isNotEmpty())
                .andExpect(jsonPath("$[1].revisionType").value("ADD"))
                .andExpect(jsonPath("$[1].fullName").value("Antes Silva"));
    }

    @Test
    void audit_unknownPatient_returns404() throws Exception {
        mockMvc.perform(get("/patients/00000000-0000-0000-0000-000000000000/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void audit_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/patients/00000000-0000-0000-0000-000000000000/audit"))
                .andExpect(status().isForbidden());
    }
}
