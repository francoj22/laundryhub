package com.microservice.payments.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequest {

    private String orderId = "order-" + UUID.randomUUID();

    @NotNull
    @DecimalMin(value = "0.50")
    private BigDecimal amount;

    @NotBlank
    private String currency = "USD";

    private String paymentMethod = "CARD";

    @JsonIgnore
    private String customerId;

    @JsonIgnore
    private String idempotencyKey;
}
