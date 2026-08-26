package com.microservice.payments.payment.gateway;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record GatewayPaymentRequest(
        Long paymentId,
        String orderId,
        String customerId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String idempotencyKey
) {
}
