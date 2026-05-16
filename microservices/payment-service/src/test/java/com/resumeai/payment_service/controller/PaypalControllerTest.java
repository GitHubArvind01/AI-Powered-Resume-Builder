package com.resumeai.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.resumeai.payment_service.dto.*;
import com.resumeai.payment_service.service.PaypalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaypalController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.gateway-url=http://localhost:8080",
        "app.client-url=http://localhost:3000",
        "app.endpoints.success-path=/api/v1/payments/pay/success",
        "app.endpoints.cancel-path=/api/v1/payments/pay/cancel",
        "app.endpoints.frontend-success-path=/payment/success",
        "app.endpoints.frontend-failed-path=/payment/failed"
})
class PaypalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaypalService paypalService;

    private Order sampleOrder;
    private PaymentResponseDTO samplePaymentResponse;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order(29.99, "USD", "paypal", "sale", "Premium Plan subscription", "MONTHLY");

        samplePaymentResponse = PaymentResponseDTO.builder()
                .id(UUID.randomUUID())
                .userId(1L)
                .paymentId("PAYID-SAMPLE123")
                .payerId("PAYER-SAMPLE456")
                .amount(BigDecimal.valueOf(29.99))
                .currency("USD")
                .description("Premium Plan subscription")
                .planType("MONTHLY")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void payment_Success_ReturnsApprovalUrl() throws Exception {
        Payment mockPayment = Mockito.mock(Payment.class);
        Links approvalLink = new Links();
        approvalLink.setRel("approval_url");
        approvalLink.setHref("https://www.paypal.com/checkout?token=12345");

        List<Links> links = new ArrayList<>();
        links.add(approvalLink);
        when(mockPayment.getLinks()).thenReturn(links);

        when(paypalService.createPayment(
                anyDouble(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyString()
        )).thenReturn(mockPayment);

        mockMvc.perform(post("/api/v1/payments/pay")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentLink").value("https://www.paypal.com/checkout?token=12345"));
    }

    @Test
    void payment_NoApprovalUrlFound_ThrowsRuntimeException() throws Exception {
        Payment mockPayment = Mockito.mock(Payment.class);
        when(mockPayment.getLinks()).thenReturn(Collections.emptyList());

        when(paypalService.createPayment(
                anyDouble(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyString()
        )).thenReturn(mockPayment);

        mockMvc.perform(post("/api/v1/payments/pay")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleOrder)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void successPay_Approved_RedirectsToFrontendSuccess() throws Exception {
        Payment mockPayment = Mockito.mock(Payment.class);
        when(mockPayment.getState()).thenReturn("approved");
        when(paypalService.executePayment(anyString(), anyString())).thenReturn(mockPayment);

        mockMvc.perform(get("/api/v1/payments/pay/success")
                        .param("paymentId", "PAYID-123")
                        .param("PayerID", "PAYER-456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/payment/success?paymentId=PAYID-123"));
    }

    @Test
    void successPay_NotApproved_RedirectsToFrontendFailed() throws Exception {
        Payment mockPayment = Mockito.mock(Payment.class);
        when(mockPayment.getState()).thenReturn("failed");
        when(paypalService.executePayment(anyString(), anyString())).thenReturn(mockPayment);

        mockMvc.perform(get("/api/v1/payments/pay/success")
                        .param("paymentId", "PAYID-123")
                        .param("PayerID", "PAYER-456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/payment/failed"));
    }

    @Test
    void cancelPay_RedirectsToFrontendFailed() throws Exception {
        mockMvc.perform(get("/api/v1/payments/pay/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/payment/failed"));
    }

    @Test
    void verifyPayment_Success_ReturnsVerificationPayload() throws Exception {
        PaymentVerificationResponseDTO responseDTO = PaymentVerificationResponseDTO.builder()
                .success(true)
                .message("Payment verified successfully.")
                .token("jwt-token-xyz")
                .payment(samplePaymentResponse)
                .build();

        when(paypalService.verifyCompletedPayment("PAYID-SAMPLE123", 1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/payments/verify/{paymentId}", "PAYID-SAMPLE123")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment verified successfully."))
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"));
    }

    @Test
    void getPaymentHistory_Success_ReturnsHistoryList() throws Exception {
        when(paypalService.getPaymentHistory(1L)).thenReturn(List.of(samplePaymentResponse));

        mockMvc.perform(get("/api/v1/payments/history")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value("PAYID-SAMPLE123"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getPaymentHistoryByStatus_Success_ReturnsFilteredHistoryList() throws Exception {
        when(paypalService.getPaymentHistoryByStatus(1L, "COMPLETED")).thenReturn(List.of(samplePaymentResponse));

        mockMvc.perform(get("/api/v1/payments/history/{status}", "COMPLETED")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getPaymentById_Success_ReturnsPaymentDetails() throws Exception {
        when(paypalService.getPaymentById("PAYID-SAMPLE123", 1L)).thenReturn(samplePaymentResponse);

        mockMvc.perform(get("/api/v1/payments/{paymentId}", "PAYID-SAMPLE123")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAYID-SAMPLE123"));
    }
}