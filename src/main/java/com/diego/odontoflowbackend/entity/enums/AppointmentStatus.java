package com.diego.odontoflowbackend.entity.enums;

public enum AppointmentStatus {
    /** Requested online by a patient; reserves the slot until the clinic confirms or rejects it. */
    PENDING,
    SCHEDULED,
    COMPLETED,
    CANCELED
}
