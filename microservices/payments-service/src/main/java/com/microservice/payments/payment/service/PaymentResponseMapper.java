package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.RefundStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PaymentResponseMapper {

    public PaymentResponse toResponse(Payment payment) {
        List<com.microservice.payments.payment.entity.Refund> refunds = payment.getRefunds() == null ? List.of() : payment.getRefunds();
        BigDecimal refundedAmount = refunds.stream()
                .filter(refund -> refund.getStatus() == RefundStatus.SUCCESS)
                .map(refund -> refund.getAmount() == null ? BigDecimal.ZERO : refund.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .refundedAmount(refundedAmount)
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .idempotencyKey(payment.getIdempotencyKey())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
