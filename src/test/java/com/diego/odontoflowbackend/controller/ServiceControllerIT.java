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
class ServiceControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Serviços IT",
                                "document", "55.555.555/0001-55",
                                "fullName", "Dra. Serv",
                                "email", "serv@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "serv@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();
    }

    @Test
    void registration_seedsDefaultServices() throws Exception {
        mockMvc.perform(get("/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void create_update_delete_service() throws Exception {
        String created = mockMvc.perform(post("/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Implante", "category", "IMPLANTOLOGY",
                                "durationMinutes", 90, "price", 1200.00))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Implante"))
                .andExpect(jsonPath("$.category").value("IMPLANTOLOGY"))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(put("/services/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Implante unitário", "category", "SURGERY",
                                "durationMinutes", 120, "price", 1500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Implante unitário"))
                .andExpect(jsonPath("$.durationMinutes").value(120));

        mockMvc.perform(delete("/services/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_invalidDuration_returns400() throws Exception {
        mockMvc.perform(post("/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "X", "category", "GENERAL", "durationMinutes", 5, "price", 10.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accessWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/services")).andExpect(status().isForbidden());
    }
}
