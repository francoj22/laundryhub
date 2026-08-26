package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final IdempotencyService service = new IdempotencyServiceImpl(paymentRepository);

    @Test
    void shouldCreateNewPaymentReservation() {
        CreatePaymentRequest request = request();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            payment.setStatus(PaymentStatus.PENDING);
            return payment;
        });

        PaymentReservation reservation = service.reservePayment(request);

        assertTrue(reservation.newlyCreated());
    }

    @Test
    void shouldReturnExistingPaymentOnDuplicateIdempotencyKey() {
        CreatePaymentRequest request = request();
        Payment existing = Payment.builder()
                .id(9L)
                .customerId("alice")
                .orderId("order-1")
                .amount(new BigDecimal("15.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.SUCCESS)
                .idempotencyKey("idem-1")
                .build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("alice", "idem-1")).thenReturn(Optional.of(existing));

        PaymentReservation reservation = service.reservePayment(request);

        assertFalse(reservation.newlyCreated());
    }

    private CreatePaymentRequest request() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId("alice");
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("15.00"));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");
        request.setIdempotencyKey("idem-1");
        return request;
    }
}
