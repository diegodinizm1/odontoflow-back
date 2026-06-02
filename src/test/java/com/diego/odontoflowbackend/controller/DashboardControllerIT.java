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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Dashboard IT",
                                "document", "50.505.050/0001-50",
                                "fullName", "Dra. Dash",
                                "email", "dash@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "dash@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();

        // a patient
        String patientBody = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Paciente Dash"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String patientId = objectMapper.readTree(patientBody).get("id").asText();

        // an appointment today
        String today = LocalDate.now().toString();
        Map<String, Object> appt = new HashMap<>();
        appt.put("patientId", patientId);
        appt.put("startTime", today + "T09:00:00");
        appt.put("endTime", today + "T10:00:00");
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appt)))
                .andExpect(status().isCreated());

        // a paid charge
        String chargeBody = mockMvc.perform(post("/charges")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId, "description", "Consulta", "amount", "200.00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String chargeId = objectMapper.readTree(chargeBody).get("id").asText();
        mockMvc.perform(patch("/charges/" + chargeId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAID"))))
                .andExpect(status().isOk());
    }

    @Test
    void summary_returnsAggregatedMetrics() throws Exception {
        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientsCount").value(1))
                .andExpect(jsonPath("$.appointmentsToday").value(1))
                .andExpect(jsonPath("$.paidThisMonth").value(200.00))
                .andExpect(jsonPath("$.todayAppointments[0].patientName").value("Paciente Dash"));
    }

    @Test
    void dashboard_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isForbidden());
    }
}
