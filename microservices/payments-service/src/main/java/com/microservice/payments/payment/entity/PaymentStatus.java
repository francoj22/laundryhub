package com.microservice.payments.payment.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED
}
