package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.dto.RefundRequest;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.entity.Refund;
import com.microservice.payments.payment.entity.RefundStatus;
import com.microservice.payments.payment.exception.PaymentNotFoundException;
import com.microservice.payments.payment.gateway.GatewayPaymentResponse;
import com.microservice.payments.payment.gateway.PaymentGateway;
import com.microservice.payments.payment.repository.PaymentRepository;
import com.microservice.payments.payment.repository.RefundRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

public interface RefundService {

    PaymentResponse refundPayment(Long paymentId, RefundRequest request);
}

@Service
class RefundServiceImpl implements RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentValidationService paymentValidationService;
    private final PaymentGateway paymentGateway;
    private final PaymentResponseMapper paymentResponseMapper;
    private final TransactionTemplate transactionTemplate;

    RefundServiceImpl(PaymentRepository paymentRepository,
                      RefundRepository refundRepository,
                      PaymentValidationService paymentValidationService,
                      PaymentGateway paymentGateway,
                      PaymentResponseMapper paymentResponseMapper,
                      PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.paymentValidationService = paymentValidationService;
        this.paymentGateway = paymentGateway;
        this.paymentResponseMapper = paymentResponseMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public PaymentResponse refundPayment(Long paymentId, RefundRequest request) {
        Payment payment = findCurrentUserPayment(paymentId);
        BigDecimal alreadyRefunded = refundRepository.sumAmountByPaymentIdAndStatus(paymentId, RefundStatus.SUCCESS);
        paymentValidationService.validateRefundRequest(payment, alreadyRefunded, request);

        Refund refund = transactionTemplate.execute(status -> {
            Payment lockedPayment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            paymentValidationService.validatePaymentOwnership(lockedPayment, currentUserId());
            Refund pendingRefund = Refund.builder()
                    .payment(lockedPayment)
                    .amount(request.getAmount())
                    .reason(request.getReason())
                    .status(RefundStatus.PENDING)
                    .build();
            lockedPayment.addRefund(pendingRefund);
            paymentRepository.save(lockedPayment);
            return pendingRefund;
        });

        GatewayPaymentResponse gatewayResponse = paymentGateway.refund(payment.getTransactionId(), request.getAmount());

        Payment updatedPayment = transactionTemplate.execute(status -> {
            Payment lockedPayment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            Refund managedRefund = refundRepository.findById(refund.getId())
                    .orElseThrow(() -> new PaymentNotFoundException("Refund not found: " + refund.getId()));
            managedRefund.setGatewayRefundId(gatewayResponse.transactionId());
            managedRefund.setStatus(gatewayResponse.success() ? RefundStatus.SUCCESS : RefundStatus.FAILED);
            refundRepository.save(managedRefund);

            if (gatewayResponse.success()) {
                BigDecimal refundedTotal = refundRepository.sumAmountByPaymentIdAndStatus(paymentId, RefundStatus.SUCCESS);
                if (refundedTotal.compareTo(lockedPayment.getAmount()) >= 0) {
                    lockedPayment.setStatus(PaymentStatus.REFUNDED);
                }
            }
            return paymentRepository.save(lockedPayment);
        });

        return paymentResponseMapper.toResponse(updatedPayment);
    }

    private Payment findCurrentUserPayment(Long paymentId) {
        return paymentRepository.findByIdAndCustomerId(paymentId, currentUserId())
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new PaymentNotFoundException("Authenticated user not found");
        }
        return authentication.getName();
    }
}
