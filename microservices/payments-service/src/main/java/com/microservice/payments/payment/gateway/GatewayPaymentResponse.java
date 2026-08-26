package com.microservice.payments.payment.gateway;

import lombok.Builder;

@Builder
public record GatewayPaymentResponse(
        boolean success,
        String transactionId,
        String message
) {
}
