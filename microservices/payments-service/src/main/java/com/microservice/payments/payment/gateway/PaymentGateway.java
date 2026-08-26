package com.microservice.payments.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayPaymentResponse processPayment(GatewayPaymentRequest request);

    GatewayPaymentResponse refund(String transactionId, BigDecimal amount);
}
