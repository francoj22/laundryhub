package com.microservice.payments.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payments.payment.dto.WebhookEventRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.entity.WebhookEventLog;
import com.microservice.payments.payment.exception.PaymentNotFoundException;
import com.microservice.payments.payment.repository.PaymentRepository;
import com.microservice.payments.payment.repository.WebhookEventLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public interface PaymentWebhookService {

    void handleWebhook(String payload, String signature);
}

@Service
class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final WebhookEventLogRepository webhookEventLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final String webhookSecret;

    PaymentWebhookServiceImpl(ObjectMapper objectMapper,
                              PaymentRepository paymentRepository,
                              WebhookEventLogRepository webhookEventLogRepository,
                              PlatformTransactionManager transactionManager,
                              @Value("${payments.security.webhook-secret}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.webhookEventLogRepository = webhookEventLogRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.webhookSecret = webhookSecret;
    }

    @Override
    public void handleWebhook(String payload, String signature) {
        validateSignature(payload, signature);
        WebhookEventRequest event = parseEvent(payload);

        transactionTemplate.executeWithoutResult(status -> {
            if (webhookEventLogRepository.findByEventId(event.getEventId()).isPresent()) {
                return;
            }

            Payment payment = resolvePayment(event);
            applyEvent(payment, event);
            paymentRepository.save(payment);
            webhookEventLogRepository.save(WebhookEventLog.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .payloadHash(sha256(payload))
                    .build());
        });
    }

    private WebhookEventRequest parseEvent(String payload) {
        try {
            WebhookEventRequest event = objectMapper.readValue(payload, WebhookEventRequest.class);
            if (event.getEventId() == null || event.getEventId().isBlank() || event.getEventType() == null || event.getEventType().isBlank()) {
                throw new IllegalArgumentException("Webhook eventId and eventType are required");
            }
            return event;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid webhook payload", ex);
        }
    }

    private void validateSignature(String payload, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            throw new AccessDeniedException("Missing webhook signature");
        }
        String expectedSignature = hmacSha256(payload, webhookSecret);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), providedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid webhook signature");
        }
    }

    private Payment resolvePayment(WebhookEventRequest event) {
        if (event.getPaymentId() != null) {
            return paymentRepository.findByIdForUpdate(event.getPaymentId())
                    .orElseThrow(() -> new PaymentNotFoundException(event.getPaymentId()));
        }
        if (event.getTransactionId() != null && !event.getTransactionId().isBlank()) {
            return paymentRepository.findByTransactionId(event.getTransactionId())
                    .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found: " + event.getTransactionId()));
        }
        throw new IllegalArgumentException("Webhook must include paymentId or transactionId");
    }

    private void applyEvent(Payment payment, WebhookEventRequest event) {
        switch (event.getEventType()) {
            case "payment.succeeded" -> payment.setStatus(PaymentStatus.SUCCESS);
            case "payment.failed" -> payment.setStatus(PaymentStatus.FAILED);
            case "payment.pending" -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.REFUNDED) {
                    payment.setStatus(PaymentStatus.PENDING);
                }
            }
            case "payment.refunded" -> payment.setStatus(PaymentStatus.REFUNDED);
            default -> throw new IllegalArgumentException("Unsupported webhook event type: " + event.getEventType());
        }
        if (event.getTransactionId() != null && !event.getTransactionId().isBlank()) {
            payment.setTransactionId(event.getTransactionId());
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to validate webhook signature", ex);
        }
    }
}
