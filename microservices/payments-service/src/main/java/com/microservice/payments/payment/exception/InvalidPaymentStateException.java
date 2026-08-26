package com.microservice.payments.payment.exception;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
