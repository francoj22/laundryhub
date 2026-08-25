package com.microservice.payments;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository repository;

    public PaymentController(PaymentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Payment> listPayments() {
        return repository.findAll();
    }

    @PostMapping
    public Payment createPayment(@RequestBody Payment payment,
                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (payment.getId() == null || payment.getId().isBlank()) {
            payment.setId(UUID.randomUUID().toString());
        }
        payment.setUserId(userId == null ? "anonymous" : userId);
        if (payment.getCurrency() == null || payment.getCurrency().isBlank()) {
            payment.setCurrency("USD");
        }
        return repository.save(payment);
    }
}
