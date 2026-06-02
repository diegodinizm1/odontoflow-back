package com.diego.odontoflowbackend.entity.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long patientsCount,
        long appointmentsToday,
        BigDecimal paidThisMonth,
        BigDecimal pendingTotal,
        List<AppointmentResponse> todayAppointments
) {}
