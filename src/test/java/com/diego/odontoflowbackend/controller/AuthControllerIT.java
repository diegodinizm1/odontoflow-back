package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void registerTenant_returnsJwtWith201() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clinicName", "Clínica IT",
                                "document", "11.111.111/0001-11",
                                "fullName", "Dr. IT",
                                "email", "it@clinica.com",
                                "password", "senha1234"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerTenant_duplicateDocument_returns409() throws Exception {
        var body = json(Map.of(
                "clinicName", "Clínica Dup",
                "document", "22.222.222/0001-22",
                "fullName", "Dr. Dup",
                "email", "dup@clinica.com",
                "password", "senha1234"
        ));

        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Clínica já cadastrada com este documento."));
    }

    @Test
    void login_withValidCredentials_returnsJwt() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clinicName", "Clínica Login",
                                "document", "33.333.333/0001-33",
                                "fullName", "Dr. Login",
                                "email", "login@clinica.com",
                                "password", "senha1234"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "login@clinica.com",
                                "password", "senha1234"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clinicName", "Clínica WrongPass",
                                "document", "44.444.444/0001-44",
                                "fullName", "Dr. WrongPass",
                                "email", "wrongpass@clinica.com",
                                "password", "senha1234"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "wrongpass@clinica.com",
                                "password", "errada"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_missingPassword_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "qualquer@teste.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }
}
