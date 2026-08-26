package com.microservice.payments.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreatePaymentRequest {

    @NotBlank
    private String orderId;

    @NotNull
    @DecimalMin(value = "0.50")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String paymentMethod;

    @JsonIgnore
    private String customerId;

    @JsonIgnore
    private String idempotencyKey;
}
