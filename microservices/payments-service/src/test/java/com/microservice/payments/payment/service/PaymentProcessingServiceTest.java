package com.microservice.payments.payment.service;

import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.gateway.GatewayPaymentResponse;
import com.microservice.payments.payment.gateway.PaymentGateway;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentProcessingServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentGateway paymentGateway = mock(PaymentGateway.class);
    private final PaymentProcessingService service = new PaymentProcessingServiceImpl(
            paymentRepository,
            paymentGateway,
            new TestTransactionManager()
    );

    @Test
    void shouldMarkPaymentSuccessfulWhenGatewayApproves() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId("order-1")
                .customerId("alice")
                .amount(new BigDecimal("20.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.PENDING)
                .idempotencyKey("idem-1")
                .build();

        when(paymentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(payment), Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.processPayment(any())).thenReturn(GatewayPaymentResponse.builder()
                .success(true)
                .transactionId("txn-1")
                .message("ok")
                .build());

        Payment processed = service.processPayment(1L);

        assertEquals(PaymentStatus.SUCCESS, processed.getStatus());
        assertEquals("txn-1", processed.getTransactionId());
    }
}
