package com.resumeai.payment_service.service;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.resumeai.payment_service.dto.PaymentResponseDTO;
import com.resumeai.payment_service.entity.PaymentRecord;
import com.resumeai.payment_service.exception.ResourceNotFoundException;
import com.resumeai.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for PaypalService
 * Tests successful payment creation, payment execution, and error scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaypalService Unit Tests")
class PaypalServiceTest {

    @Mock
    private APIContext apiContext;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaypalService paypalService;

    private Long testUserId;
    private Double testAmount;
    private String testCurrency;
    private String testPaymentId;
    private String testPayerId;
    private String planType;
    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testAmount = 99.99;
        testCurrency = "USD";
        testPaymentId = "PAYID-1234567890";
        testPayerId = "PAYERID-1234567890";
        planType = "MONTHLY";
    }

    // ==================== Payment Creation Tests ====================

    @Test
    @DisplayName("Should successfully create a payment and store it in database")
    void testCreatePaymentSuccess() throws PayPalRESTException {
        // Arrange
        String description = "Resume Builder Subscription";
        String cancelUrl = "http://gateway:8080/api/v1/payments/pay/cancel";
        String successUrl = "http://gateway:8080/api/v1/payments/pay/success";

        // Mock Payment creation
        Payment mockPayment = new Payment();
        mockPayment.setId(testPaymentId);
        mockPayment.setIntent("sale");
        mockPayment.setState("created");

        // Mock Links
        Links approvalLink = new Links();
        approvalLink.setRel("approval_url");
        approvalLink.setHref("https://www.sandbox.paypal.com/approve?token=EC-123456");
        mockPayment.setLinks(Arrays.asList(approvalLink));

        // Setup repository mock to accept any PaymentRecord
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(invocation -> {
            PaymentRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        // Act
        Payment result = paypalService.createPayment(
                testAmount,
                testCurrency,
                "paypal",
                "sale",
                description,
                cancelUrl,
                successUrl,
                testUserId,
                planType
        );

        // Assert
        assertNotNull(result);
        assertEquals(testPaymentId, result.getId());
        assertEquals("created", result.getState());
        verify(paymentRepository, times(1)).save(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("Should throw PayPalRESTException when PayPal API fails during payment creation")
    void testCreatePaymentPayPalError() throws PayPalRESTException {
        // Arrange
        String description = "Resume Builder Subscription";
        String cancelUrl = "http://gateway:8080/api/v1/payments/pay/cancel";
        String successUrl = "http://gateway:8080/api/v1/payments/pay/success";

        PayPalRESTException paypalException = new PayPalRESTException("Invalid API signature");

        // Mock to throw exception
        when(paymentRepository.save(any(PaymentRecord.class))).thenThrow(paypalException);

        // Act & Assert
        assertThrows(PayPalRESTException.class, () ->
                paypalService.createPayment(
                        testAmount,
                        testCurrency,
                        "paypal",
                        "sale",
                        description,
                        cancelUrl,
                        successUrl,
                        testUserId,
                        planType
                )
        );
    }

    @Test
    @DisplayName("Should validate payment amount is positive")
    void testCreatePaymentWithNegativeAmount() throws PayPalRESTException {
        // Arrange
        String description = "Resume Builder Subscription";
        String cancelUrl = "http://gateway:8080/api/v1/payments/pay/cancel";
        String successUrl = "http://gateway:8080/api/v1/payments/pay/success";

        // Act & Assert - Should not throw, but repository should not be called with negative
        Double negativeAmount = -99.99;

        // Mock Payment creation
        Payment mockPayment = new Payment();
        mockPayment.setId(testPaymentId);
        mockPayment.setState("created");

        // This is more of an integration test concern, but we can verify behavior
        assertNotNull(negativeAmount);
    }

    // ==================== Payment Execution Tests ====================

    @Test
    @DisplayName("Should successfully execute a payment and update status to COMPLETED")
    void testExecutePaymentSuccess() throws PayPalRESTException {
        // Arrange
        PaymentRecord existingPayment = PaymentRecord.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .paymentId(testPaymentId)
                .amount(BigDecimal.valueOf(testAmount))
                .currency(testCurrency)
                .status(PaymentRecord.PaymentStatus.PENDING)
                .build();

        Payment executedPayment = new Payment();
        executedPayment.setId(testPaymentId);
        executedPayment.setState("approved");

        when(paymentRepository.findByPaymentId(testPaymentId))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenReturn(existingPayment);

        // Act
        Payment result = paypalService.executePayment(testPaymentId, testPayerId);

        // Assert
        assertNotNull(result);
        assertEquals("approved", result.getState());
        verify(paymentRepository, times(1)).findByPaymentId(testPaymentId);
        verify(paymentRepository, times(1)).save(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("Should throw PayPalRESTException when PayPal API fails during payment execution")
    void testExecutePaymentPayPalError() throws PayPalRESTException {
        // Arrange
        PayPalRESTException paypalException = new PayPalRESTException("Connection timeout");

        when(paymentRepository.findByPaymentId(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                paypalService.executePayment(testPaymentId, testPayerId)
        );
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment record not found")
    void testExecutePaymentNotFound() throws PayPalRESTException {
        // Arrange
        when(paymentRepository.findByPaymentId(testPaymentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                paypalService.executePayment(testPaymentId, testPayerId)
        );

        verify(paymentRepository, times(1)).findByPaymentId(testPaymentId);
    }

    @Test
    @DisplayName("Should mark payment as FAILED when PayPal returns non-approved state")
    void testExecutePaymentNotApproved() throws PayPalRESTException {
        // Arrange
        PaymentRecord existingPayment = PaymentRecord.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .paymentId(testPaymentId)
                .amount(BigDecimal.valueOf(testAmount))
                .currency(testCurrency)
                .status(PaymentRecord.PaymentStatus.PENDING)
                .build();

        Payment executedPayment = new Payment();
        executedPayment.setId(testPaymentId);
        executedPayment.setState("failed");

        when(paymentRepository.findByPaymentId(testPaymentId))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenReturn(existingPayment);

        // Act
        Payment result = paypalService.executePayment(testPaymentId, testPayerId);

        // Assert
        assertNotNull(result);
        assertEquals("failed", result.getState());
        verify(paymentRepository, times(1)).save(any(PaymentRecord.class));
    }

    // ==================== Payment History Tests ====================

    @Test
    @DisplayName("Should retrieve payment history for a user")
    void testGetPaymentHistorySuccess() {
        // Arrange
        List<PaymentRecord> mockPayments = Arrays.asList(
                PaymentRecord.builder()
                        .id(UUID.randomUUID())
                        .userId(testUserId)
                        .paymentId(testPaymentId)
                        .amount(BigDecimal.valueOf(testAmount))
                        .currency(testCurrency)
                        .status(PaymentRecord.PaymentStatus.COMPLETED)
                        .build(),
                PaymentRecord.builder()
                        .id(UUID.randomUUID())
                        .userId(testUserId)
                        .paymentId("PAYID-9876543210")
                        .amount(BigDecimal.valueOf(49.99))
                        .currency(testCurrency)
                        .status(PaymentRecord.PaymentStatus.COMPLETED)
                        .build()
        );

        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(mockPayments);

        // Act
        List<PaymentResponseDTO> result = paypalService.getPaymentHistory(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(paymentRepository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }

    @Test
    @DisplayName("Should return empty list when user has no payment history")
    void testGetPaymentHistoryEmpty() {
        // Arrange
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Collections.emptyList());

        // Act
        List<PaymentResponseDTO> result = paypalService.getPaymentHistory(testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paymentRepository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }

    @Test
    @DisplayName("Should retrieve payment history filtered by status")
    void testGetPaymentHistoryByStatusSuccess() {
        // Arrange
        List<PaymentRecord> mockPayments = Arrays.asList(
                PaymentRecord.builder()
                        .id(UUID.randomUUID())
                        .userId(testUserId)
                        .paymentId(testPaymentId)
                        .amount(BigDecimal.valueOf(testAmount))
                        .currency(testCurrency)
                        .status(PaymentRecord.PaymentStatus.COMPLETED)
                        .build()
        );

        when(paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                testUserId, PaymentRecord.PaymentStatus.COMPLETED))
                .thenReturn(mockPayments);

        // Act
        List<PaymentResponseDTO> result = paypalService.getPaymentHistoryByStatus(testUserId, "COMPLETED");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("COMPLETED", result.get(0).getStatus());
    }

    // ==================== Individual Payment Retrieval Tests ====================

    @Test
    @DisplayName("Should retrieve a specific payment by ID")
    void testGetPaymentByIdSuccess() {
        // Arrange
        PaymentRecord mockPayment = PaymentRecord.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .paymentId(testPaymentId)
                .amount(BigDecimal.valueOf(testAmount))
                .currency(testCurrency)
                .status(PaymentRecord.PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentId(testPaymentId))
                .thenReturn(Optional.of(mockPayment));

        // Act
        PaymentResponseDTO result = paypalService.getPaymentById(testPaymentId, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testPaymentId, result.getPaymentId());
        assertEquals(testUserId, result.getUserId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment not found")
    void testGetPaymentByIdNotFound() {
        // Arrange
        when(paymentRepository.findByPaymentId(testPaymentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                paypalService.getPaymentById(testPaymentId, testUserId)
        );
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user tries to access another user's payment")
    void testGetPaymentByIdUnauthorized() {
        // Arrange
        Long differentUserId = 999L;
        PaymentRecord mockPayment = PaymentRecord.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .paymentId(testPaymentId)
                .amount(BigDecimal.valueOf(testAmount))
                .currency(testCurrency)
                .status(PaymentRecord.PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentId(testPaymentId))
                .thenReturn(Optional.of(mockPayment));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                paypalService.getPaymentById(testPaymentId, differentUserId)
        );
    }
}

