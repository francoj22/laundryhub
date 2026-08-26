package com.microservice.payments.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
