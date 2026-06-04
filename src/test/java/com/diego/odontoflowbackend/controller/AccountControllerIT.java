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
class AccountControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Conta IT", "document", "33.333.333/0001-33",
                                "fullName", "Dra. Conta", "email", "conta@clinica.com", "password", "senha1234"))))
                .andExpect(status().isCreated());
        token = login("senha1234");
    }

    private String login(String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "conta@clinica.com", "password", password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void changePassword_wrongCurrent_returns401() throws Exception {
        mockMvc.perform(patch("/account/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "errada", "newPassword", "novaSenha123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_requiresAuth_returns403() throws Exception {
        mockMvc.perform(patch("/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha1234", "newPassword", "novaSenha123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_success_thenLoginWithNewPassword() throws Exception {
        // use a dedicated account so other tests keep the original password
        mockMvc.perform(post("/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Recep Troca", "email", "troca@clinica.com",
                                "role", "RECEPTIONIST", "password", "senha1234"))))
                .andExpect(status().isCreated());
        String recBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "troca@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        String recToken = objectMapper.readTree(recBody).get("token").asText();

        mockMvc.perform(patch("/account/password").header("Authorization", "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha1234", "newPassword", "novaSenha123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "troca@clinica.com", "password", "novaSenha123"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "troca@clinica.com", "password", "senha1234"))))
                .andExpect(status().isUnauthorized());
    }
}
