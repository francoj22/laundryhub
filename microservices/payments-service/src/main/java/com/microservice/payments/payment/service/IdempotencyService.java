package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.exception.DuplicatePaymentException;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface IdempotencyService {

    Optional<Payment> findExistingPayment(String customerId, String idempotencyKey);

    PaymentReservation reservePayment(CreatePaymentRequest request);
}

record PaymentReservation(Payment payment, boolean newlyCreated) {
}

@Service
class IdempotencyServiceImpl implements IdempotencyService {

    private final PaymentRepository paymentRepository;

    IdempotencyServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Optional<Payment> findExistingPayment(String customerId, String idempotencyKey) {
        return paymentRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
    }

    @Override
    @Transactional
    public PaymentReservation reservePayment(CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        try {
            return new PaymentReservation(paymentRepository.saveAndFlush(payment), true);
        } catch (DataIntegrityViolationException ex) {
            Payment existing = paymentRepository.findByCustomerIdAndIdempotencyKey(request.getCustomerId(), request.getIdempotencyKey())
                    .orElseThrow(() -> new DuplicatePaymentException("Duplicate payment request detected", ex));
            return new PaymentReservation(existing, false);
        }
    }
}
