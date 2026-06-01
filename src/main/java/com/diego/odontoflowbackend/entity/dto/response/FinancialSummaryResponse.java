package com.diego.odontoflowbackend.entity.dto.response;

import java.math.BigDecimal;

public record FinancialSummaryResponse(
        int year,
        int month,
        BigDecimal paidThisMonth,
        BigDecimal pendingTotal
) {}
