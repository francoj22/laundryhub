package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentValidationService validationService = mock(PaymentValidationService.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final PaymentProcessingService processingService = mock(PaymentProcessingService.class);
    private final PaymentResponseMapper paymentResponseMapper = new PaymentResponseMapper();
    private final PaymentService service = new PaymentServiceImpl(
            paymentRepository,
            validationService,
            idempotencyService,
            processingService,
            paymentResponseMapper,
            new TestTransactionManager()
    );

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnExistingPaymentForDuplicateRequest() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        CreatePaymentRequest request = request();
        Payment existing = payment(5L, PaymentStatus.SUCCESS);
        when(idempotencyService.findExistingPayment("alice", "idem-1")).thenReturn(Optional.of(existing));

        PaymentResponse response = service.createPayment(request);

        assertEquals(5L, response.getId());
        verify(idempotencyService, never()).reservePayment(any());
    }

    @Test
    void shouldProcessNewPayment() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        CreatePaymentRequest request = request();
        Payment pending = payment(6L, PaymentStatus.PENDING);
        Payment success = payment(6L, PaymentStatus.SUCCESS);
        success.setTransactionId("txn-6");

        when(idempotencyService.findExistingPayment("alice", "idem-1")).thenReturn(Optional.empty());
        when(idempotencyService.reservePayment(any())).thenReturn(new PaymentReservation(pending, true));
        when(processingService.processPayment(6L)).thenReturn(success);

        PaymentResponse response = service.createPayment(request);

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("txn-6", response.getTransactionId());
    }

    private CreatePaymentRequest request() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("20.00"));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");
        request.setIdempotencyKey("idem-1");
        return request;
    }

    private Payment payment(Long id, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .orderId("order-1")
                .customerId("alice")
                .amount(new BigDecimal("20.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(status)
                .idempotencyKey("idem-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
