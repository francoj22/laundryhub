package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.dto.RefundRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.entity.Refund;
import com.microservice.payments.payment.entity.RefundStatus;
import com.microservice.payments.payment.gateway.GatewayPaymentResponse;
import com.microservice.payments.payment.gateway.PaymentGateway;
import com.microservice.payments.payment.repository.PaymentRepository;
import com.microservice.payments.payment.repository.RefundRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefundServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final RefundRepository refundRepository = mock(RefundRepository.class);
    private final PaymentValidationService validationService = mock(PaymentValidationService.class);
    private final PaymentGateway paymentGateway = mock(PaymentGateway.class);
    private final RefundService service = new RefundServiceImpl(
            paymentRepository,
            refundRepository,
            validationService,
            paymentGateway,
            new PaymentResponseMapper(),
            new TestTransactionManager()
    );

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRefundPaymentAndMarkFullyRefundedWhenAmountMatchesCapture() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        Payment payment = Payment.builder()
                .id(2L)
                .orderId("order-2")
                .customerId("alice")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-2")
                .idempotencyKey("idem-2")
                .build();
        RefundRequest request = new RefundRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setReason("customer request");

        when(paymentRepository.findByIdAndCustomerId(2L, "alice")).thenReturn(Optional.of(payment));
        when(refundRepository.sumAmountByPaymentIdAndStatus(2L, RefundStatus.SUCCESS)).thenReturn(BigDecimal.ZERO, new BigDecimal("10.00"));
        when(paymentRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(payment), Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            if (!saved.getRefunds().isEmpty() && saved.getRefunds().getFirst().getId() == null) {
                saved.getRefunds().getFirst().setId(99L);
            }
            return saved;
        });
        when(refundRepository.findById(99L)).thenAnswer(invocation -> Optional.of(payment.getRefunds().getFirst()));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.refund("txn-2", new BigDecimal("10.00"))).thenReturn(GatewayPaymentResponse.builder()
                .success(true)
                .transactionId("rfnd-2")
                .message("ok")
                .build());

        PaymentResponse response = service.refundPayment(2L, request);

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("10.00"), response.getRefundedAmount());
    }
}
