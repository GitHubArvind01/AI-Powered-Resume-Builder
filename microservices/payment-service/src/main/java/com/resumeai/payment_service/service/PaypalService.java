package com.resumeai.payment_service.service;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.resumeai.payment_service.dto.PaymentResponseDTO;
import com.resumeai.payment_service.entity.PaymentRecord;
import com.resumeai.payment_service.exception.ResourceNotFoundException;
import com.resumeai.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * PaypalService - Handles PayPal payment operations and database persistence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalService {

    private final APIContext apiContext;
    private final PaymentRepository paymentRepository;

    /**
     * Creates a payment in PayPal and stores the record in database
     */
    @Transactional
    public Payment createPayment(
            Double total,
            String currency,
            String method,
            String intent,
            String description,
            String cancelUrl,
            String successUrl,
            Long userId) throws PayPalRESTException {

        log.info("Creating payment for user {} with amount {}", userId, total);

        // Create PayPal Amount
        Amount amount = new Amount();
        amount.setCurrency(currency);
        String formattedTotal = String.format(Locale.forLanguageTag("en-US"), "%.2f", total);
        amount.setTotal(formattedTotal);

        // Create Transaction
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        // Create Payer
        Payer payer = new Payer();
        payer.setPaymentMethod(method);

        // Create Payment
        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        // Set Redirect URLs
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        // Create payment with PayPal
        Payment createdPayment = payment.create(apiContext);
        log.info("PayPal payment created with ID: {}", createdPayment.getId());

        // Store payment record in database
        PaymentRecord paymentRecord = PaymentRecord.builder()
                .userId(userId)
                .paymentId(createdPayment.getId())
                .payerId("") // Will be updated after approval
                .amount(BigDecimal.valueOf(total))
                .currency(currency)
                .description(description)
                .status(PaymentRecord.PaymentStatus.PENDING)
                .build();

        paymentRepository.save(paymentRecord);
        log.info("Payment record saved to database for user {} with payment ID {}", userId, createdPayment.getId());

        return createdPayment;
    }

    /**
     * Executes a payment and updates the record status to COMPLETED
     */
    @Transactional
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {

        log.info("Executing payment with ID: {} and Payer ID: {}", paymentId, payerId);

        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);

        Payment executedPayment = payment.execute(apiContext, paymentExecute);
        log.info("Payment executed successfully. State: {}", executedPayment.getState());

        // Update payment record status
        PaymentRecord paymentRecord = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if ("approved".equalsIgnoreCase(executedPayment.getState())) {
            paymentRecord.setStatus(PaymentRecord.PaymentStatus.COMPLETED);
            paymentRecord.setPayerId(payerId);
            paymentRepository.save(paymentRecord);
            log.info("Payment record updated to COMPLETED for payment ID: {}", paymentId);
        } else {
            paymentRecord.setStatus(PaymentRecord.PaymentStatus.FAILED);
            paymentRepository.save(paymentRecord);
            log.warn("Payment approval failed. State: {}", executedPayment.getState());
        }

        return executedPayment;
    }

    /**
     * Fetches payment history for a specific user
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentHistory(Long userId) {

        log.info("Fetching payment history for user: {}", userId);

        List<PaymentRecord> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (payments.isEmpty()) {
            log.warn("No payments found for user: {}", userId);
        }

        return payments.stream()
                .map(PaymentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Fetches payment history for a user by status
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentHistoryByStatus(Long userId, String status) {

        log.info("Fetching payment history for user {} with status: {}", userId, status);

        PaymentRecord.PaymentStatus paymentStatus = PaymentRecord.PaymentStatus.valueOf(status.toUpperCase());

        List<PaymentRecord> payments = paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, paymentStatus);

        return payments.stream()
                .map(PaymentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific payment by ID
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(String paymentId, Long userId) {

        log.info("Fetching payment with ID: {} for user: {}", paymentId, userId);

        PaymentRecord payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized access to payment: " + paymentId);
        }

        return PaymentResponseDTO.fromEntity(payment);
    }
}

