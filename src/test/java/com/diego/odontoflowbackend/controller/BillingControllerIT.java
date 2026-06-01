package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillingControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeAll
    void setup() throws Exception {
        mockMvc.perform(post("/auth/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clinicName", "Clínica Billing IT",
                                "document", "20.202.020/0001-20",
                                "fullName", "Dra. Billing",
                                "email", "billing@clinica.com",
                                "password", "senha1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "billing@clinica.com", "password", "senha1234"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).get("token").asText();
    }

    @Test @Order(1)
    void plans_returnsAllTiers() throws Exception {
        mockMvc.perform(get("/billing/plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test @Order(2)
    void subscription_lazilyCreatesFree() throws Exception {
        mockMvc.perform(get("/billing/subscription").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(3)
    void subscribePro_activatesAndCreatesPaidInvoice() throws Exception {
        mockMvc.perform(post("/billing/subscribe")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "PRO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPeriodEnd").isNotEmpty());

        mockMvc.perform(get("/billing/invoices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].amount").value(199.90));
    }

    @Test @Order(4)
    void cancel_setsCanceled() throws Exception {
        mockMvc.perform(post("/billing/cancel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test @Order(5)
    void webhook_isPublic() throws Exception {
        // unknown invoice id -> handled (404 from service), but endpoint itself is reachable without a token
        mockMvc.perform(post("/webhooks/billing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "invoice.paid", "externalInvoiceId", "does-not-exist"))))
                .andExpect(status().isNotFound());
    }

    @Test @Order(6)
    void billing_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/billing/subscription"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(7)
    void simulateTriggers_driveSubscriptionStatus() throws Exception {
        // subscribe to get a fresh PRO invoice
        mockMvc.perform(post("/billing/subscribe")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "PRO"))))
                .andExpect(status().isOk());

        String invoices = mockMvc.perform(get("/billing/invoices").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String invoiceId = objectMapper.readTree(invoices).get(0).get("id").asText();

        // simulate failed payment -> PAST_DUE
        mockMvc.perform(post("/billing/simulate/" + invoiceId + "/fail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAST_DUE"));

        // simulate recovery -> ACTIVE
        mockMvc.perform(post("/billing/simulate/" + invoiceId + "/recover")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
