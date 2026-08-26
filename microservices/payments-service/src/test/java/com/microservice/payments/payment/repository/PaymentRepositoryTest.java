package com.microservice.payments.payment.repository;

import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldFindPaymentByCustomerAndIdempotencyKey() {
        Payment payment = Payment.builder()
                .orderId("order-1")
                .customerId("alice")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.PENDING)
                .idempotencyKey("idem-1")
                .build();
        paymentRepository.saveAndFlush(payment);

        Optional<Payment> found = paymentRepository.findByCustomerIdAndIdempotencyKey("alice", "idem-1");

        assertTrue(found.isPresent());
    }
}
