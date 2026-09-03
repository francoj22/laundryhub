package com.microservice.payments.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.dto.RefundRequest;
import com.microservice.payments.payment.service.PaymentService;
import com.microservice.payments.payment.service.PaymentWebhookService;
import com.microservice.payments.payment.service.RefundService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentController(PaymentService paymentService,
                             RefundService refundService,
                             PaymentWebhookService paymentWebhookService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request,
                                                         @RequestHeader("Idempotency-Key") String idempotencyKey) {
        request.setIdempotencyKey(idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    @GetMapping
    public List<PaymentResponse> listPayments() {
        return paymentService.listPayments();
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable Long paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @PostMapping("/{paymentId}/confirm")
    public PaymentResponse confirmPayment(@PathVariable Long paymentId) {
        return paymentService.confirmPayment(paymentId);
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentResponse cancelPayment(@PathVariable Long paymentId) {
        return paymentService.cancelPayment(paymentId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponse refundPayment(@PathVariable Long paymentId,
                                         @Valid @RequestBody RefundRequest request) {
        return refundService.refundPayment(paymentId, request);
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void handleWebhook(@RequestBody String payload,
                              @RequestHeader("X-Webhook-Signature") String signature) {
        paymentWebhookService.handleWebhook(payload, signature);
    }
}
