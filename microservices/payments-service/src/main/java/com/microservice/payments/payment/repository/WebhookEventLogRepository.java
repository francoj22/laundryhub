package com.microservice.payments.payment.repository;

import com.microservice.payments.payment.entity.WebhookEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {

    Optional<WebhookEventLog> findByEventId(String eventId);
}
