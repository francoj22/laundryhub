package com.microservice.payments.payment.service;

import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.exception.InvalidPaymentStateException;
import com.microservice.payments.payment.exception.PaymentFailedException;
import com.microservice.payments.payment.exception.PaymentNotFoundException;
import com.microservice.payments.payment.gateway.GatewayPaymentRequest;
import com.microservice.payments.payment.gateway.GatewayPaymentResponse;
import com.microservice.payments.payment.gateway.PaymentGateway;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public interface PaymentProcessingService {

    Payment processPayment(Long paymentId);
}

record ProcessingStart(Payment payment, boolean shouldCallGateway) {
}

@Service
class PaymentProcessingServiceImpl implements PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    PaymentProcessingServiceImpl(PaymentRepository paymentRepository,
                                 PaymentGateway paymentGateway,
                                 PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Payment processPayment(Long paymentId) {
        ProcessingStart processingStart = markProcessing(paymentId);
        if (!processingStart.shouldCallGateway()) {
            return processingStart.payment();
        }

        GatewayPaymentResponse response;
        try {
            Payment payment = processingStart.payment();
            response = paymentGateway.processPayment(GatewayPaymentRequest.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .customerId(payment.getCustomerId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .paymentMethod(payment.getPaymentMethod())
                    .idempotencyKey(payment.getIdempotencyKey())
                    .build());
        } catch (RuntimeException ex) {
            markGatewayFailure(paymentId, ex.getMessage());
            throw new PaymentFailedException("Payment gateway processing failed", ex);
        }

        return finalizeProcessing(paymentId, response);
    }

    private ProcessingStart markProcessing(Long paymentId) {
        return transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            return switch (payment.getStatus()) {
                case PENDING, FAILED -> {
                    payment.setStatus(PaymentStatus.PROCESSING);
                    yield new ProcessingStart(paymentRepository.save(payment), true);
                }
                case PROCESSING -> new ProcessingStart(payment, false);
                case SUCCESS, CANCELLED, REFUNDED -> throw new InvalidPaymentStateException(
                        "Cannot process payment in status " + payment.getStatus());
            };
        });
    }

    private void markGatewayFailure(Long paymentId, String failureMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            payment.setStatus(PaymentStatus.FAILED);
            if (failureMessage != null && !failureMessage.isBlank()) {
                payment.setTransactionId("failed:" + failureMessage.substring(0, Math.min(100, failureMessage.length())));
            }
            paymentRepository.save(payment);
        });
    }

    private Payment finalizeProcessing(Long paymentId, GatewayPaymentResponse response) {
        return transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            payment.setTransactionId(response.transactionId());
            payment.setStatus(response.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            return paymentRepository.save(payment);
        });
    }
}
