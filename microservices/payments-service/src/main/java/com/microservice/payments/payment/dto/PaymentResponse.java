package com.microservice.payments.payment.dto;

import com.microservice.payments.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentResponse {
    Long id;
    String orderId;
    String customerId;
    BigDecimal amount;
    BigDecimal refundedAmount;
    String currency;
    String paymentMethod;
    PaymentStatus status;
    String transactionId;
    String idempotencyKey;
    Instant createdAt;
    Instant updatedAt;
}
