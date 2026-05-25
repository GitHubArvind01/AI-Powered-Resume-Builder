package com.resumeai.payment_service.service;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.resumeai.payment_service.config.AuthClient;
import com.resumeai.payment_service.dto.*;
import com.resumeai.payment_service.entity.PaymentRecord;
import com.resumeai.payment_service.exception.ResourceNotFoundException;
import com.resumeai.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaypalServiceTest {

    @Mock
    private APIContext apiContext;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private PaypalService paypalService;

    private PaymentRecord pendingRecord;
    private PaymentRecord completedRecord;

    @BeforeEach
    void setUp() {
        pendingRecord = PaymentRecord.builder()
                .id(UUID.randomUUID())
                .userId(1L)
                .paymentId("PAYID-123")
                .payerId("")
                .amount(BigDecimal.valueOf(29.99))
                .currency("USD")
                .description("Test Description")
                .planType("MONTHLY")
                .status(PaymentRecord.PaymentStatus.PENDING)
                .build();

        completedRecord = PaymentRecord.builder()
                .id(pendingRecord.getId())
                .userId(1L)
                .paymentId("PAYID-123")
                .payerId("PAYER-456")
                .amount(BigDecimal.valueOf(29.99))
                .currency("USD")
                .description("Test Description")
                .planType("MONTHLY")
                .status(PaymentRecord.PaymentStatus.COMPLETED)
                .build();
    }

    @Test
    void createPayment_Success_SavesPendingRecord() throws PayPalRESTException {
        Payment mockCreatedPayment = mock(Payment.class);
        when(mockCreatedPayment.getId()).thenReturn("PAYID-123");

        try (MockedConstruction<Payment> mocked = mockConstruction(Payment.class, (mock, context) -> {
            when(mock.create(any(APIContext.class))).thenReturn(mockCreatedPayment);
        })) {
            Payment result = paypalService.createPayment(
                    29.99, "USD", "paypal", "sale", "Test Description",
                    "http://cancel.com", "http://success.com", 1L, "MONTHLY"
            );

            assertNotNull(result);
            assertEquals("PAYID-123", result.getId());
            verify(paymentRepository, times(1)).save(any(PaymentRecord.class));
        }
    }

    @Test
    void createPayment_UnsupportedPlanType_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            paypalService.createPayment(
                    29.99, "USD", "paypal", "sale", "Test Description",
                    "http://cancel.com", "http://success.com", 1L, "INVALID_PLAN"
            );
        });
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void executePayment_Approved_UpdatesStatusToCompleted() throws PayPalRESTException {
        Payment mockExecutedPayment = mock(Payment.class);
        when(mockExecutedPayment.getState()).thenReturn("approved");

        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(pendingRecord));

        try (MockedConstruction<Payment> mocked = mockConstruction(Payment.class, (mock, context) -> {
            when(mock.execute(any(APIContext.class), any(PaymentExecution.class))).thenReturn(mockExecutedPayment);
        })) {
            Payment result = paypalService.executePayment("PAYID-123", "PAYER-456");

            assertNotNull(result);
            assertEquals("approved", result.getState());
            assertEquals(PaymentRecord.PaymentStatus.COMPLETED, pendingRecord.getStatus());
            assertEquals("PAYER-456", pendingRecord.getPayerId());
            verify(paymentRepository, times(1)).save(pendingRecord);
        }
    }

    @Test
    void executePayment_NotApproved_UpdatesStatusToFailed() throws PayPalRESTException {
        Payment mockExecutedPayment = mock(Payment.class);
        when(mockExecutedPayment.getState()).thenReturn("failed");

        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(pendingRecord));

        try (MockedConstruction<Payment> mocked = mockConstruction(Payment.class, (mock, context) -> {
            when(mock.execute(any(APIContext.class), any(PaymentExecution.class))).thenReturn(mockExecutedPayment);
        })) {
            Payment result = paypalService.executePayment("PAYID-123", "PAYER-456");

            assertNotNull(result);
            assertEquals("failed", result.getState());
            assertEquals(PaymentRecord.PaymentStatus.FAILED, pendingRecord.getStatus());
            verify(paymentRepository, times(1)).save(pendingRecord);
        }
    }

    @Test
    void executePayment_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(paymentRepository.findByPaymentId("PAYID-ABSENT")).thenReturn(Optional.empty());

        // Act & Assert
        try (MockedConstruction<Payment> mocked = mockConstruction(Payment.class, (mock, context) -> {
            // Stub the execute method to prevent it from failing with an internal PayPal exception if called
            when(mock.execute(any(APIContext.class), any(PaymentExecution.class))).thenReturn(mock(Payment.class));
        })) {

            assertThrows(ResourceNotFoundException.class, () -> {
                paypalService.executePayment("PAYID-ABSENT", "PAYER-456");
            });

        }
    }

    @Test
    void getPaymentHistory_Success_ReturnsMappedList() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(completedRecord));

        List<PaymentResponseDTO> history = paypalService.getPaymentHistory(1L);

        assertFalse(history.isEmpty());
        assertEquals("PAYID-123", history.get(0).getPaymentId());
    }

    @Test
    void getPaymentHistoryByStatus_Success_ReturnsFilteredList() {
        when(paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, PaymentRecord.PaymentStatus.COMPLETED))
                .thenReturn(List.of(completedRecord));

        List<PaymentResponseDTO> history = paypalService.getPaymentHistoryByStatus(1L, "COMPLETED");

        assertFalse(history.isEmpty());
        assertEquals("COMPLETED", history.get(0).getStatus());
    }

    @Test
    void getPaymentById_Authorized_ReturnsDto() {
        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(completedRecord));

        PaymentResponseDTO response = paypalService.getPaymentById("PAYID-123", 1L);

        assertNotNull(response);
        assertEquals("PAYID-123", response.getPaymentId());
    }

    @Test
    void getPaymentById_Unauthorized_ThrowsResourceNotFoundException() {
        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(completedRecord));

        assertThrows(ResourceNotFoundException.class, () -> {
            paypalService.getPaymentById("PAYID-123", 999L); // Wrong User ID
        });
    }

    @Test
    void verifyCompletedPayment_Success_CallsAuthClientAndReturnsPayload() {
        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(completedRecord));

        CurrentUserResponseDTO userResponse = new CurrentUserResponseDTO();
        userResponse.setId(1L);
        AuthResponseDTO authBody = new AuthResponseDTO("new-jwt-token", "Success", userResponse);
        when(authClient.updateSubscription(any(SubscriptionUpdateRequest.class)))
                .thenReturn(ResponseEntity.ok(authBody));

        PaymentVerificationResponseDTO response = paypalService.verifyCompletedPayment("PAYID-123", 1L);

        assertTrue(response.isSuccess());
        assertEquals("new-jwt-token", response.getToken());
        assertNotNull(response.getUser());
        verify(authClient, times(1)).updateSubscription(any(SubscriptionUpdateRequest.class));
    }

    @Test
    void verifyCompletedPayment_NotCompletedYet_ThrowsResourceNotFoundException() {
        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(pendingRecord));

        assertThrows(ResourceNotFoundException.class, () -> {
            paypalService.verifyCompletedPayment("PAYID-123", 1L);
        });
    }

    @Test
    void verifyCompletedPayment_AuthResponseInvalid_ThrowsResourceNotFoundException() {
        when(paymentRepository.findByPaymentId("PAYID-123")).thenReturn(Optional.of(completedRecord));
        when(authClient.updateSubscription(any(SubscriptionUpdateRequest.class)))
                .thenReturn(ResponseEntity.ok(new AuthResponseDTO(null, "Error", null)));

        assertThrows(ResourceNotFoundException.class, () -> {
            paypalService.verifyCompletedPayment("PAYID-123", 1L);
        });
    }
}