package com.microservice.payments.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.gateway.MockPaymentGateway;
import com.microservice.payments.payment.repository.PaymentRepository;
import com.microservice.payments.payment.repository.WebhookEventLogRepository;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentWebhookServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final WebhookEventLogRepository webhookEventLogRepository = mock(WebhookEventLogRepository.class);
    private final PaymentWebhookService service = new PaymentWebhookServiceImpl(
            new ObjectMapper(),
            paymentRepository,
            webhookEventLogRepository,
            new TestTransactionManager(),
            "test-webhook-secret"
    );

    @Test
    void shouldApplySucceededWebhookOnce() throws Exception {
        Payment payment = Payment.builder()
                .id(7L)
                .orderId("order-7")
                .customerId("alice")
                .amount(new BigDecimal("11.00"))
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.PENDING)
                .idempotencyKey("idem-7")
                .build();
        String payload = "{" +
                "\"eventId\":\"evt-1\"," +
                "\"eventType\":\"payment.succeeded\"," +
                "\"paymentId\":7," +
                "\"transactionId\":\"txn-7\"}";

        when(webhookEventLogRepository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handleWebhook(payload, sign(payload, "test-webhook-secret"));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(webhookEventLogRepository).save(any());
    }

    private String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
