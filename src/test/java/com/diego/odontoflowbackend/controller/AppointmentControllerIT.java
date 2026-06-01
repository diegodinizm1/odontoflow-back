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
class AppointmentControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String patientId;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Agenda IT",
                                "document", "66.666.666/0001-66",
                                "fullName", "Dr. Agenda",
                                "email", "agenda@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "agenda@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();

        String patientBody = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Paciente Agenda"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        patientId = objectMapper.readTree(patientBody).get("id").asText();
    }

    private Map<String, Object> appointment(String start, String end) {
        Map<String, Object> m = new HashMap<>();
        m.put("patientId", patientId);
        m.put("startTime", start);
        m.put("endTime", end);
        return m;
    }

    @Test
    void create_returns201() throws Exception {
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-07-01T09:00:00", "2026-07-01T10:00:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.patientName").value("Paciente Agenda"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void create_overlap_returns409() throws Exception {
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-07-02T14:00:00", "2026-07-02T15:00:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-07-02T14:30:00", "2026-07-02T15:30:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void create_endBeforeStart_returns400() throws Exception {
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-07-03T11:00:00", "2026-07-03T10:00:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_inRange_returnsAppointments() throws Exception {
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-08-10T08:00:00", "2026-08-10T09:00:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .param("start", "2026-08-10T00:00:00")
                        .param("end", "2026-08-11T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].patientName").value("Paciente Agenda"));
    }

    @Test
    void reschedule_returns200() throws Exception {
        String body = mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-09-01T09:00:00", "2026-09-01T10:00:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(put("/appointments/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startTime", "2026-09-01T15:00:00",
                                "endTime", "2026-09-01T16:00:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("2026-09-01T15:00:00"));
    }

    @Test
    void cancel_viaStatus_returns200() throws Exception {
        String body = mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointment("2026-10-01T09:00:00", "2026-10-01T10:00:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(patch("/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void accessWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/appointments")
                        .param("start", "2026-07-01T00:00:00")
                        .param("end", "2026-07-02T00:00:00"))
                .andExpect(status().isForbidden());
    }
}
