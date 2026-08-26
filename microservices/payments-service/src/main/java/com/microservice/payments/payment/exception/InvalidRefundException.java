package com.microservice.payments.payment.exception;

public class InvalidRefundException extends RuntimeException {

    public InvalidRefundException(String message) {
        super(message);
    }
}
