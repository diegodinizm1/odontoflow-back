package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.response.DashboardResponse;
import com.diego.odontoflowbackend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated clinic overview")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Clinic overview: counts, monthly revenue and today's agenda")
    public DashboardResponse summary() {
        return dashboardService.summary();
    }
}
