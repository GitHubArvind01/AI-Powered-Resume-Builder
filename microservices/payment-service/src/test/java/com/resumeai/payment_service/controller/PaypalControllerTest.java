package com.resumeai.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.payment_service.TestConfig;
import com.resumeai.payment_service.dto.Order;
import com.resumeai.payment_service.dto.PaymentResponseDTO;
import com.resumeai.payment_service.service.PaypalService;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for PaypalController
 * Tests payment endpoints with Spring Boot test context
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@DisplayName("PaypalController Integration Tests")
class PaypalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaypalService paypalService;

    private Long testUserId;
    private String testPaymentId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testPaymentId = "PAYID-1234567890";

        testOrder = new Order();
        testOrder.setPrice(99.99);
        testOrder.setCurrency("USD");
        testOrder.setMethod("paypal");
        testOrder.setIntent("sale");
        testOrder.setDescription("Resume Builder Subscription");
    }

    // ==================== Payment Creation Tests ====================

    @Test
    @DisplayName("Should create payment and return approval URL")
    void testPaymentSuccess() throws Exception {
        // Arrange
        Payment mockPayment = new Payment();
        mockPayment.setId(testPaymentId);
        mockPayment.setState("created");

        Links approvalLink = new Links();
        approvalLink.setRel("approval_url");
        approvalLink.setHref("https://www.sandbox.paypal.com/approve?token=EC-123456");
        mockPayment.setLinks(Arrays.asList(approvalLink));

        when(paypalService.createPayment(
                anyDouble(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(testUserId),
                anyString()
        )).thenReturn(mockPayment);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", testUserId)
                .content(objectMapper.writeValueAsString(testOrder)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://www.sandbox.paypal.com/approve")));
    }

    @Test
    @DisplayName("Should return 400 when Order validation fails")
    void testPaymentValidationError() throws Exception {
        // Arrange - Invalid order (negative price)
        Order invalidOrder = new Order();
        invalidOrder.setPrice(-10.0);
        invalidOrder.setCurrency("USD");
        invalidOrder.setMethod("paypal");
        invalidOrder.setIntent("sale");
        invalidOrder.setDescription("Invalid Order");

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", testUserId)
                .content(objectMapper.writeValueAsString(invalidOrder)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    void testPaymentMissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testOrder)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 502 when PayPal API fails")
    void testPaymentPayPalError() throws Exception {
        // Arrange
        when(paypalService.createPayment(
                anyDouble(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(testUserId),
                anyString()
        )).thenThrow(new PayPalRESTException("Invalid API signature"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", testUserId)
                .content(objectMapper.writeValueAsString(testOrder)))
                .andExpect(status().isBadGateway());
    }

    // ==================== Payment History Tests ====================

    @Test
    @DisplayName("Should retrieve payment history for user")
    void testGetPaymentHistorySuccess() throws Exception {
        // Arrange
        PaymentResponseDTO payment = PaymentResponseDTO.builder()
                .paymentId(testPaymentId)
                .userId(testUserId)
                .amount(BigDecimal.valueOf(99.99))
                .currency("USD")
                .status("COMPLETED")
                .build();

        when(paypalService.getPaymentHistory(testUserId))
                .thenReturn(Arrays.asList(payment));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/history")
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(testPaymentId))
                .andExpect(jsonPath("$[0].userId").value(testUserId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should return empty list when no payment history")
    void testGetPaymentHistoryEmpty() throws Exception {
        // Arrange
        when(paypalService.getPaymentHistory(testUserId))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/history")
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    // ==================== Payment History by Status Tests ====================

    @Test
    @DisplayName("Should retrieve payment history filtered by status")
    void testGetPaymentHistoryByStatusSuccess() throws Exception {
        // Arrange
        PaymentResponseDTO payment = PaymentResponseDTO.builder()
                .paymentId(testPaymentId)
                .userId(testUserId)
                .amount(BigDecimal.valueOf(99.99))
                .currency("USD")
                .status("COMPLETED")
                .build();

        when(paypalService.getPaymentHistoryByStatus(testUserId, "COMPLETED"))
                .thenReturn(Arrays.asList(payment));

        // Act & Asserts
        mockMvc.perform(get("/api/v1/payments/history/COMPLETED")
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    // ==================== Get Payment by ID Tests ====================

    @Test
    @DisplayName("Should retrieve a specific payment by ID")
    void testGetPaymentByIdSuccess() throws Exception {
        // Arrange
        PaymentResponseDTO payment = PaymentResponseDTO.builder()
                .paymentId(testPaymentId)
                .userId(testUserId)
                .amount(BigDecimal.valueOf(99.99))
                .currency("USD")
                .status("COMPLETED")
                .build();

        when(paypalService.getPaymentById(testPaymentId, testUserId))
                .thenReturn(payment);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/" + testPaymentId)
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(testPaymentId))
                .andExpect(jsonPath("$.amount").value(99.99));
    }

    // ==================== Payment Success/Cancel Tests ====================

    @Test
    @DisplayName("Should redirect to frontend success page on payment approval")
    void testSuccessPaymentApproved() throws Exception {
        // Arrange
        Payment approvedPayment = new Payment();
        approvedPayment.setId(testPaymentId);
        approvedPayment.setState("approved");

        when(paypalService.executePayment(testPaymentId, "PAYERID123"))
                .thenReturn(approvedPayment);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/pay/success")
                .param("paymentId", testPaymentId)
                .param("PayerID", "PAYERID123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("*payment-success*"));
    }

    @Test
    @DisplayName("Should redirect to frontend cancel page on payment cancellation")
    void testCancelPayment() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/pay/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("*payment-failed*"));
    }
}
