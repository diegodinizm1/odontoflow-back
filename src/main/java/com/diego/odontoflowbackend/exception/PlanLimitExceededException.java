package com.diego.odontoflowbackend.exception;

/** Thrown when an action would exceed the tenant's current subscription plan limits. */
public class PlanLimitExceededException extends RuntimeException {
    public PlanLimitExceededException(String message) {
        super(message);
    }
}
