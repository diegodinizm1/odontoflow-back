package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.entity.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        String plan,
        String planName,
        BigDecimal monthlyPrice,
        SubscriptionStatus status,
        LocalDate currentPeriodEnd,
        int maxPatients,
        int maxDentists
) {
    public static SubscriptionResponse from(Subscription s) {
        Plan p = s.getPlan();
        return new SubscriptionResponse(
                p.name(), p.getDisplayName(), p.getMonthlyPrice(),
                s.getStatus(), s.getCurrentPeriodEnd(), p.getMaxPatients(), p.getMaxDentists());
    }
}
