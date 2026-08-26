package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.dto.RefundRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.exception.InvalidPaymentStateException;
import com.microservice.payments.payment.exception.InvalidRefundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentValidationServiceTest {

    private final PaymentValidationService service = new PaymentValidationServiceImpl();

    @Test
    void shouldNormalizeValidPaymentRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId("alice");
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("25.00"));
        request.setCurrency("usd");
        request.setPaymentMethod("card");
        request.setIdempotencyKey("idem-1");

        service.validateCreateRequest(request);

        assertEquals("USD", request.getCurrency());
        assertEquals("CARD", request.getPaymentMethod());
    }

    @Test
    void shouldRejectUnsupportedPaymentMethod() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId("alice");
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("25.00"));
        request.setCurrency("USD");
        request.setPaymentMethod("CRYPTO");
        request.setIdempotencyKey("idem-1");

        assertThrows(InvalidPaymentStateException.class, () -> service.validateCreateRequest(request));
    }

    @Test
    void shouldRejectRefundAboveRemainingAmount() {
        Payment payment = Payment.builder()
                .customerId("alice")
                .orderId("order-1")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.SUCCESS)
                .idempotencyKey("idem-1")
                .build();
        RefundRequest request = new RefundRequest();
        request.setAmount(new BigDecimal("30.00"));
        request.setReason("Customer request");

        assertThrows(InvalidRefundException.class,
                () -> service.validateRefundRequest(payment, new BigDecimal("25.00"), request));
    }
}
