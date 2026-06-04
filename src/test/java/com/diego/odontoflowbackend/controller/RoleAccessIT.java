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

/** A receptionist must not reach billing, finances or team management. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RoleAccessIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String dentistToken;
    private String receptionistToken;
    private String patientId;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Papéis IT",
                                "document", "44.444.444/0001-44",
                                "fullName", "Dra. Dona",
                                "email", "dona@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());
        dentistToken = login("dona@clinica.com");

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + dentistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Recep Rita", "email", "rita@clinica.com",
                                "role", "RECEPTIONIST", "password", "senha1234"))))
                .andExpect(status().isCreated());
        receptionistToken = login("rita@clinica.com");

        String patientBody = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + dentistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Paciente Caixa"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        patientId = objectMapper.readTree(patientBody).get("id").asText();
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void receptionist_isBlockedFromRevenueBillingAndTeamMutations() throws Exception {
        // the revenue summary (faturamento) stays dentist-only
        mockMvc.perform(get("/charges/summary").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/billing/plans").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/users").header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "X", "email", "x@c.com", "role", "RECEPTIONIST", "password", "senha1234"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionist_canListTeamAndChargesForFrontDesk() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/charges").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());
    }

    @Test
    void receptionist_canCreateChargeForPatient() throws Exception {
        mockMvc.perform(post("/charges").header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId, "description", "Consulta", "amount", 120.00))))
                .andExpect(status().isCreated());
    }

    @Test
    void dentist_hasAccess() throws Exception {
        mockMvc.perform(get("/charges").header("Authorization", "Bearer " + dentistToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/billing/plans").header("Authorization", "Bearer " + dentistToken))
                .andExpect(status().isOk());
    }
}
