package com.microservice.payments.payment.repository;

import com.microservice.payments.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    Optional<Payment> findByIdAndCustomerId(Long id, String customerId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);
}
