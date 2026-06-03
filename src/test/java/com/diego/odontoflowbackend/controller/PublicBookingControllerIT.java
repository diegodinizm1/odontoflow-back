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
class PublicBookingControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String slug;
    private String dentistId;
    private String serviceId;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Pública IT",
                                "document", "77.777.777/0001-77",
                                "fullName", "Dra. Pública",
                                "email", "publica@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "publica@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();

        String dashBody = mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        slug = objectMapper.readTree(dashBody).get("publicSlug").asText();

        String usersBody = mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        dentistId = objectMapper.readTree(usersBody).get(0).get("id").asText();

        // services are auto-seeded on registration; grab one from the public profile
        String profileBody = mockMvc.perform(get("/public/clinics/{slug}", slug))
                .andReturn().getResponse().getContentAsString();
        serviceId = objectMapper.readTree(profileBody).get("services").get(0).get("id").asText();
    }

    @Test
    void directory_listsClinics() throws Exception {
        mockMvc.perform(get("/public/clinics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.publicSlug == '" + slug + "')].clinicName").value(
                        org.hamcrest.Matchers.hasItem("Clínica Pública IT")));
    }

    @Test
    void clinicProfile_isPublic_andListsDentistAndServices() throws Exception {
        mockMvc.perform(get("/public/clinics/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinicName").value("Clínica Pública IT"))
                .andExpect(jsonPath("$.dentists[0].id").value(dentistId))
                .andExpect(jsonPath("$.services").isArray())
                .andExpect(jsonPath("$.services[0].durationMinutes").isNumber());
    }

    @Test
    void unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/public/clinics/{slug}", "nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void availability_listsFreeSlots() throws Exception {
        mockMvc.perform(get("/public/clinics/{slug}/availability", slug)
                        .param("dentistId", dentistId)
                        .param("serviceId", serviceId)
                        .param("date", "2027-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots[0]").value("08:00"));
    }

    @Test
    void booking_createsPendingAppointment_andBlocksTheSlot() throws Exception {
        Map<String, Object> booking = Map.of(
                "dentistId", dentistId,
                "serviceId", serviceId,
                "date", "2027-04-20",
                "time", "09:00",
                "patientName", "Paciente Online",
                "patientPhone", "11988887777");

        mockMvc.perform(post("/public/clinics/{slug}/bookings", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.dentistName").value("Dra. Pública"));

        // the clinic sees the pending request in its agenda
        mockMvc.perform(get("/appointments").header("Authorization", "Bearer " + token)
                        .param("start", "2027-04-20T00:00:00")
                        .param("end", "2027-04-21T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].patientName").value("Paciente Online"));

        // the same slot is now taken
        mockMvc.perform(post("/public/clinics/{slug}/bookings", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isConflict());
    }
}
