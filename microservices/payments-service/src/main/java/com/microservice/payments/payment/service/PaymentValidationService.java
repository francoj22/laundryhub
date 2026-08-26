package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.dto.RefundRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.exception.InvalidPaymentStateException;
import com.microservice.payments.payment.exception.InvalidRefundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public interface PaymentValidationService {

    void validateCreateRequest(CreatePaymentRequest request);

    void validatePaymentOwnership(Payment payment, String currentUserId);

    void validatePaymentState(Payment payment, Set<PaymentStatus> allowedStates, String action);

    void validateRefundRequest(Payment payment, BigDecimal alreadyRefunded, RefundRequest request);
}

@Service
class PaymentValidationServiceImpl implements PaymentValidationService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");
    private static final Set<String> SUPPORTED_METHODS = Set.of("CARD", "WALLET", "BANK_TRANSFER");

    @Override
    public void validateCreateRequest(CreatePaymentRequest request) {
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new InvalidPaymentStateException("Authenticated customer is required");
        }
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new InvalidPaymentStateException("Order is required");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new InvalidPaymentStateException("Payment amount must be greater than zero");
        }
        String currency = request.getCurrency() == null ? "" : request.getCurrency().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new InvalidPaymentStateException("Unsupported currency: " + request.getCurrency());
        }
        String paymentMethod = request.getPaymentMethod() == null ? "" : request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_METHODS.contains(paymentMethod)) {
            throw new InvalidPaymentStateException("Unsupported payment method: " + request.getPaymentMethod());
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new InvalidPaymentStateException("Idempotency-Key header is required");
        }

        request.setCurrency(currency);
        request.setPaymentMethod(paymentMethod);
    }

    @Override
    public void validatePaymentOwnership(Payment payment, String currentUserId) {
        if (!payment.getCustomerId().equals(currentUserId)) {
            throw new InvalidPaymentStateException("Payment does not belong to the authenticated user");
        }
    }

    @Override
    public void validatePaymentState(Payment payment, Set<PaymentStatus> allowedStates, String action) {
        if (!allowedStates.contains(payment.getStatus())) {
            throw new InvalidPaymentStateException("Cannot " + action + " payment in status " + payment.getStatus());
        }
    }

    @Override
    public void validateRefundRequest(Payment payment, BigDecimal alreadyRefunded, RefundRequest request) {
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new InvalidRefundException("Only successful payments can be refunded");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new InvalidRefundException("Refund amount must be greater than zero");
        }
        BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);
        if (remaining.signum() <= 0) {
            throw new InvalidRefundException("Payment is already fully refunded");
        }
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new InvalidRefundException("Refund amount exceeds remaining captured amount");
        }
    }
}
