package com.microservice.payments.payment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_customer_idempotency", columnNames = {"customer_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "order_id"),
                @Index(name = "idx_payment_customer_id", columnList = "customer_id"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_transaction_id", columnList = "transaction_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "transaction_id", unique = true, length = 128)
    private String transactionId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    public void addRefund(Refund refund) {
        refunds.add(refund);
        refund.setPayment(this);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
