package com.microservice.payments.payment.repository;

import com.microservice.payments.payment.entity.Refund;
import com.microservice.payments.payment.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findAllByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId and r.status = :status")
    BigDecimal sumAmountByPaymentIdAndStatus(@Param("paymentId") Long paymentId, @Param("status") RefundStatus status);
}
