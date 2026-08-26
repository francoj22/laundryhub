package com.microservice.payments.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WebhookEventRequest {

    @NotBlank
    private String eventId;

    @NotBlank
    private String eventType;

    private Long paymentId;

    private String transactionId;

    private BigDecimal amount;
}
