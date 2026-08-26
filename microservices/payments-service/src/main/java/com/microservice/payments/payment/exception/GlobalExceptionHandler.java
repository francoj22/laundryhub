package com.microservice.payments.payment.exception;

import com.microservice.payments.payment.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "PAYMENT_FAILED", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicatePaymentException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidPaymentStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATE", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidRefundException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefund(InvalidRefundException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REFUND", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (left, right) -> left));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String code,
                                                String message,
                                                HttpServletRequest request,
                                                Map<String, String> details) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build());
    }
}
