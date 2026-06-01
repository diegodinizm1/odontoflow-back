package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.enums.Plan;

import java.math.BigDecimal;

public record PlanResponse(
        String code,
        String name,
        BigDecimal monthlyPrice,
        int maxPatients,
        int maxDentists
) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.name(), p.getDisplayName(), p.getMonthlyPrice(), p.getMaxPatients(), p.getMaxDentists());
    }
}
