package com.microservice.payments.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "webhook_event_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_event_id", columnNames = "event_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        processedAt = Instant.now();
    }
}
