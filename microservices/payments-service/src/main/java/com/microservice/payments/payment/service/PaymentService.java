package com.microservice.payments.payment.service;

import com.microservice.payments.payment.dto.CreatePaymentRequest;
import com.microservice.payments.payment.dto.PaymentResponse;
import com.microservice.payments.payment.entity.Payment;
import com.microservice.payments.payment.entity.PaymentStatus;
import com.microservice.payments.payment.exception.PaymentNotFoundException;
import com.microservice.payments.payment.repository.PaymentRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPayment(Long paymentId);

    PaymentResponse confirmPayment(Long paymentId);

    PaymentResponse cancelPayment(Long paymentId);

    List<PaymentResponse> listPayments();
}

@Service
class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentValidationService paymentValidationService;
    private final IdempotencyService idempotencyService;
    private final PaymentProcessingService paymentProcessingService;
    private final PaymentResponseMapper paymentResponseMapper;
    private final TransactionTemplate transactionTemplate;

    PaymentServiceImpl(PaymentRepository paymentRepository,
                       PaymentValidationService paymentValidationService,
                       IdempotencyService idempotencyService,
                       PaymentProcessingService paymentProcessingService,
                       PaymentResponseMapper paymentResponseMapper,
                       PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.paymentValidationService = paymentValidationService;
        this.idempotencyService = idempotencyService;
        this.paymentProcessingService = paymentProcessingService;
        this.paymentResponseMapper = paymentResponseMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        request.setCustomerId(currentUserId());
        paymentValidationService.validateCreateRequest(request);

        return idempotencyService.findExistingPayment(request.getCustomerId(), request.getIdempotencyKey())
                .map(paymentResponseMapper::toResponse)
                .orElseGet(() -> {
                    PaymentReservation reservation = idempotencyService.reservePayment(request);
                    if (!reservation.newlyCreated()) {
                        return paymentResponseMapper.toResponse(reservation.payment());
                    }
                    Payment processedPayment = paymentProcessingService.processPayment(reservation.payment().getId());
                    return paymentResponseMapper.toResponse(processedPayment);
                });
    }

    @Override
    public PaymentResponse getPayment(Long paymentId) {
        return paymentResponseMapper.toResponse(findCurrentUserPayment(paymentId));
    }

    @Override
    public PaymentResponse confirmPayment(Long paymentId) {
        Payment payment = findCurrentUserPayment(paymentId);
        paymentValidationService.validatePaymentState(payment, Set.of(PaymentStatus.PENDING, PaymentStatus.FAILED), "confirm");
        return paymentResponseMapper.toResponse(paymentProcessingService.processPayment(payment.getId()));
    }

    @Override
    public PaymentResponse cancelPayment(Long paymentId) {
        return transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            paymentValidationService.validatePaymentOwnership(payment, currentUserId());
            paymentValidationService.validatePaymentState(payment, Set.of(PaymentStatus.PENDING, PaymentStatus.FAILED), "cancel");
            payment.setStatus(PaymentStatus.CANCELLED);
            return paymentResponseMapper.toResponse(paymentRepository.save(payment));
        });
    }

    @Override
    public List<PaymentResponse> listPayments() {
        return paymentRepository.findAllByCustomerIdOrderByCreatedAtDesc(currentUserId()).stream()
                .map(paymentResponseMapper::toResponse)
                .toList();
    }

    private Payment findCurrentUserPayment(Long paymentId) {
        String currentUserId = currentUserId();
        return paymentRepository.findByIdAndCustomerId(paymentId, currentUserId)
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
