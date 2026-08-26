package com.microservice.payments.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayPaymentResponse processPayment(GatewayPaymentRequest request) {
        String normalizedMethod = request.paymentMethod().toUpperCase(Locale.ROOT);
        boolean success = request.amount().compareTo(new BigDecimal("10000.00")) <= 0
                && !normalizedMethod.equals("DECLINED")
                && !request.orderId().toLowerCase(Locale.ROOT).startsWith("fail-");

        return GatewayPaymentResponse.builder()
                .success(success)
                .transactionId(success ? "txn_" + UUID.randomUUID() : null)
                .message(success ? "Payment authorized" : "Mock gateway declined the payment")
                .build();
    }

    @Override
    public GatewayPaymentResponse refund(String transactionId, BigDecimal amount) {
        boolean success = transactionId != null && !transactionId.isBlank() && amount.signum() > 0;
        return GatewayPaymentResponse.builder()
                .success(success)
                .transactionId(success ? "rfnd_" + UUID.randomUUID() : null)
                .message(success ? "Refund accepted" : "Refund rejected")
                .build();
    }
}
