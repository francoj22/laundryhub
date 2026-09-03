package com.microservice.payments.payment.controller;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.exception.GlobalExceptionHandler;
import com.microservice.payments.payment.service.PaymentService;
import com.microservice.payments.payment.service.PaymentWebhookService;
import com.microservice.payments.payment.service.RefundService;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private RefundService refundService;

    @MockitoBean
    private PaymentWebhookService paymentWebhookService;

    @Test
    void shouldCreatePayment() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("19.99"));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");

        when(paymentService.createPayment(any())).thenReturn(PaymentResponse.builder()
                .id(1L)
                .orderId("order-1")
                .customerId("alice")
                .amount(new BigDecimal("19.99"))
                .refundedAmount(BigDecimal.ZERO)
                .currency("USD")
                .paymentMethod("CARD")
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-1")
                .idempotencyKey("idem-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldRejectRequestWhenIdempotencyKeyHeaderIsMissing() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("19.99"));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
