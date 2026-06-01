package com.diego.odontoflowbackend.entity.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum Plan {
    FREE("Grátis", new BigDecimal("0.00"), 50, 1),
    ESSENTIAL("Essencial", new BigDecimal("89.90"), 500, 3),
    PRO("Pro", new BigDecimal("199.90"), 1_000_000, 1_000_000);

    private final String displayName;
    private final BigDecimal monthlyPrice;
    private final int maxPatients;
    private final int maxDentists;

    Plan(String displayName, BigDecimal monthlyPrice, int maxPatients, int maxDentists) {
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.maxPatients = maxPatients;
        this.maxDentists = maxDentists;
    }

    public boolean isPaid() {
        return monthlyPrice.signum() > 0;
    }
}
