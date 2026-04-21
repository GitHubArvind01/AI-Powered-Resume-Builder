package com.resumeai.payment_service.controller;

import com.resumeai.payment_service.dto.Order;
import com.resumeai.payment_service.dto.PaymentResponseDTO;
import com.resumeai.payment_service.service.PaypalService;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

/**
 * PaypalController - Handles payment operations via PayPal
 * Endpoints: /api/v1/payments
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Slf4j
@Tag(name = "Payments", description = "Payment management operations")
public class PaypalController {

    private final PaypalService service;

    // Injecting base URLs
    @Value("${app.gateway-url}")
    private String gatewayUrl;

    @Value("${app.client-url}")
    private String clientUrl;

    // Injecting Paths
    @Value("${app.endpoints.success-path}")
    private String successPath;

    @Value("${app.endpoints.cancel-path}")
    private String cancelPath;

    @Value("${app.endpoints.frontend-success-path}")
    private String frontendSuccessPath;

    @Value("${app.endpoints.frontend-failed-path}")
    private String frontendFailedPath;

    /**
     * Create a payment
     */
    @PostMapping("/pay")
    @Operation(summary = "Create a new payment", description = "Initiates a PayPal payment and returns the approval URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment created successfully, returns approval URL"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "502", description = "PayPal API error")
    })
    public ResponseEntity<String> payment(
            @Valid @RequestBody Order order,
            @RequestHeader(value = "X-User-Id", required = true) Long userId) throws PayPalRESTException {

        log.info("Payment request received for user: {} with amount: {}", userId, order.getPrice());

        String backendSuccessUrl = gatewayUrl + successPath;
        String backendCancelUrl = gatewayUrl + cancelPath;

        Payment payment = service.createPayment(
                order.getPrice(),
                order.getCurrency(),
                order.getMethod(),
                order.getIntent(),
                order.getDescription(),
                backendCancelUrl,
                backendSuccessUrl,
                userId
        );

        // Extract approval URL from PayPal response
        for (Links link : payment.getLinks()) {
            if (link.getRel().equals("approval_url")) {
                log.info("Approval URL generated for user: {}", userId);
                return ResponseEntity.ok(link.getHref());
            }
        }

        throw new RuntimeException("PayPal approval URL not found in response");
    }

    /**
     * Handle successful payment
     */
    @GetMapping("/pay/success")
    @Operation(summary = "Handle successful payment", description = "Completes the payment and redirects to frontend success page")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to frontend success page"),
            @ApiResponse(responseCode = "502", description = "PayPal API error")
    })
    public RedirectView successPay(
            @Parameter(description = "PayPal Payment ID", required = true)
            @RequestParam("paymentId") String paymentId,
            @Parameter(description = "PayPal Payer ID", required = true)
            @RequestParam("PayerID") String payerId) throws PayPalRESTException {

        log.info("Payment success callback received - paymentId: {}, payerId: {}", paymentId, payerId);

        Payment payment = service.executePayment(paymentId, payerId);
        if (payment.getState().equals("approved")) {
            log.info("Payment approved successfully for paymentId: {}", paymentId);
            return new RedirectView(clientUrl + frontendSuccessPath);
        }

        log.warn("Payment not approved for paymentId: {}", paymentId);
        return new RedirectView(clientUrl + frontendFailedPath);
    }

    /**
     * Handle cancelled payment
     */
    @GetMapping("/pay/cancel")
    @Operation(summary = "Handle cancelled payment", description = "Redirects to frontend failure page when user cancels payment")
    @ApiResponse(responseCode = "302", description = "Redirect to frontend failure page")
    public RedirectView cancelPay() {

        log.info("Payment cancelled by user");
        return new RedirectView(clientUrl + frontendFailedPath);
    }

    /**
     * Get payment history for a user
     */
    @GetMapping("/history")
    @Operation(summary = "Get payment history", description = "Retrieves all payments for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - X-User-Id header missing")
    })
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentHistory(
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {

        log.info("Fetching payment history for user: {}", userId);
        List<PaymentResponseDTO> payments = service.getPaymentHistory(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment history filtered by status
     */
    @GetMapping("/history/{status}")
    @Operation(summary = "Get payment history by status", description = "Retrieves payments filtered by status (PENDING, COMPLETED, CANCELLED, FAILED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - X-User-Id header missing")
    })
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentHistoryByStatus(
            @Parameter(description = "Payment status", example = "COMPLETED")
            @PathVariable String status,
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {

        log.info("Fetching payment history for user {} with status: {}", userId, status);
        List<PaymentResponseDTO> payments = service.getPaymentHistoryByStatus(userId, status);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get a specific payment by ID
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "Retrieves details of a specific payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - X-User-Id header missing")
    })
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @Parameter(description = "PayPal Payment ID", required = true)
            @PathVariable String paymentId,
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {

        log.info("Fetching payment {} for user: {}", paymentId, userId);
        PaymentResponseDTO payment = service.getPaymentById(paymentId, userId);
        return ResponseEntity.ok(payment);
    }
}
