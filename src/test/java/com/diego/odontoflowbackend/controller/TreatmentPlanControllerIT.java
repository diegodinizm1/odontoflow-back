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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TreatmentPlanControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String patientId;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Plano IT",
                                "document", "40.404.040/0001-40",
                                "fullName", "Dra. Plano",
                                "email", "plano@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "plano@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();

        String patientBody = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Paciente Plano"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        patientId = objectMapper.readTree(patientBody).get("id").asText();
    }

    private String createPlan() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Reabilitação",
                "items", List.of(
                        Map.of("description", "Canal", "tooth", "36", "amount", "1250.00"),
                        Map.of("description", "Restauração", "tooth", "26", "amount", "340.00"))));
        return mockMvc.perform(post("/patients/" + patientId + "/treatment-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROPOSED"))
                .andExpect(jsonPath("$.total").value(1590.00))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void create_returns201WithItemsAndTotal() throws Exception {
        createPlan();
    }

    @Test
    void completeItem_generatesChargeInFinances() throws Exception {
        String plan = createPlan();
        var node = objectMapper.readTree(plan);
        String planId = node.get("id").asText();
        String itemId = node.get("items").get(0).get("id").asText();

        mockMvc.perform(post("/patients/" + patientId + "/treatment-plans/" + planId + "/items/" + itemId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.items[0].status").value("DONE"))
                .andExpect(jsonPath("$.items[0].chargeId").isNotEmpty());

        // a pending charge now exists in finances
        mockMvc.perform(get("/charges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Canal (dente 36)"))
                .andExpect(jsonPath("$[0].amount").value(1250.00))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void create_unknownPatient_returns404() throws Exception {
        mockMvc.perform(post("/patients/00000000-0000-0000-0000-000000000000/treatment-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "X",
                                "items", List.of(Map.of("description", "a", "amount", "10.00"))))))
                .andExpect(status().isNotFound());
    }

    @Test
    void accessWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/patients/" + patientId + "/treatment-plans"))
                .andExpect(status().isForbidden());
    }
}
