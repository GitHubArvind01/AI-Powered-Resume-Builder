package com.resumeai.payment_service.service;

import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.resumeai.payment_service.config.AuthClient;
import com.resumeai.payment_service.dto.AuthResponseDTO;
import com.resumeai.payment_service.dto.PaymentResponseDTO;
import com.resumeai.payment_service.dto.PaymentVerificationResponseDTO;
import com.resumeai.payment_service.dto.SubscriptionUpdateRequest;
import com.resumeai.payment_service.entity.PaymentRecord;
import com.resumeai.payment_service.exception.ResourceNotFoundException;
import com.resumeai.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalService {

    private final APIContext apiContext;
    private final PaymentRepository paymentRepository;
    private final AuthClient authClient;

    @Transactional
    public Payment createPayment(
            Double total,
            String currency,
            String method,
            String intent,
            String description,
            String cancelUrl,
            String successUrl,
            Long userId,
            String planType) throws PayPalRESTException {

        log.info("Creating payment for user {} with amount {} and plan {}", userId, total, planType);

        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setTotal(String.format(Locale.forLanguageTag("en-US"), "%.2f", total));

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod(method);

        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment = payment.create(apiContext);

        PaymentRecord paymentRecord = PaymentRecord.builder()
                .userId(userId)
                .paymentId(createdPayment.getId())
                .payerId("")
                .amount(BigDecimal.valueOf(total))
                .currency(currency)
                .description(description)
                .planType(normalizePlanType(planType))
                .status(PaymentRecord.PaymentStatus.PENDING)
                .build();

        paymentRepository.save(paymentRecord);
        return createdPayment;
    }

    @Transactional
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        log.info("Executing payment with ID: {} and Payer ID: {}", paymentId, payerId);

        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);

        Payment executedPayment = payment.execute(apiContext, paymentExecute);

        PaymentRecord paymentRecord = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if ("approved".equalsIgnoreCase(executedPayment.getState())) {
            paymentRecord.setStatus(PaymentRecord.PaymentStatus.COMPLETED);
            paymentRecord.setPayerId(payerId);
        } else {
            paymentRecord.setStatus(PaymentRecord.PaymentStatus.FAILED);
        }

        paymentRepository.save(paymentRecord);
        return executedPayment;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentHistoryByStatus(Long userId, String status) {
        PaymentRecord.PaymentStatus paymentStatus = PaymentRecord.PaymentStatus.valueOf(status.toUpperCase());
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, paymentStatus).stream()
                .map(PaymentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(String paymentId, Long userId) {
        PaymentRecord payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized access to payment: " + paymentId);
        }

        return PaymentResponseDTO.fromEntity(payment);
    }

    @Transactional
    public PaymentVerificationResponseDTO verifyCompletedPayment(String paymentId, Long userId) {
        PaymentRecord payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized access to payment: " + paymentId);
        }

        if (payment.getStatus() != PaymentRecord.PaymentStatus.COMPLETED) {
            throw new ResourceNotFoundException("Payment is not completed yet for ID: " + paymentId);
        }

        ResponseEntity<AuthResponseDTO> authResponse = authClient.updateSubscription(
                new SubscriptionUpdateRequest(
                        userId,
                        payment.getPlanType(),
                        payment.getStatus().name(),
                        payment.getPaymentId()
                )
        );

        AuthResponseDTO body = authResponse.getBody();
        if (body == null || body.getToken() == null || body.getUser() == null) {
            throw new ResourceNotFoundException("Unable to refresh authenticated user state after payment verification.");
        }

        return PaymentVerificationResponseDTO.builder()
                .success(true)
                .message("Payment verified successfully.")
                .token(body.getToken())
                .user(body.getUser())
                .payment(PaymentResponseDTO.fromEntity(payment))
                .build();
    }

    private String normalizePlanType(String planType) {
        String normalized = planType == null ? "MONTHLY" : planType.trim().toUpperCase();
        if (!"MONTHLY".equals(normalized) && !"YEARLY".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported plan type: " + planType);
        }
        return normalized;
    }
}
