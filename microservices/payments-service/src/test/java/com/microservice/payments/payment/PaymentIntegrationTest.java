package com.microservice.payments.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldCreatePaymentAndPreventDuplicateChargeByIdempotencyKey() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-int-1");
        request.setAmount(new BigDecimal("22.50"));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");

        mockMvc.perform(post("/payments")
                        .header("X-User-Id", "alice")
                        .header("X-User-Role", "user")
                        .header("Idempotency-Key", "idem-int-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/payments")
                        .header("X-User-Id", "alice")
                        .header("X-User-Role", "user")
                        .header("Idempotency-Key", "idem-int-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/payments")
                        .header("X-User-Id", "alice")
                        .header("X-User-Role", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-int-1"));

        assertEquals(1, paymentRepository.count());
    }
}
