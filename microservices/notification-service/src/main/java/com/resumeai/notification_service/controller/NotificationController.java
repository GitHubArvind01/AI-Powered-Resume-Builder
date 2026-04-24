package com.resumeai.notification_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.notification_service.dto.EmailEvent;
import com.resumeai.notification_service.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Internal-only controller for health verification and manual email testing.
 * <p>
 * In production this should be protected behind the API Gateway and not
 * exposed publicly. Normal email flow goes through RabbitMQ — not this
 * controller.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Service", description = "Internal notification endpoints")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final EmailService emailService;

    /**
     * Lightweight health-check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running ✅");
    }

    /**
     * Manually trigger an email — useful for smoke-testing without RabbitMQ.
     * Remove or secure this endpoint before deploying to production.
     */
    @PostMapping("/send-test-email")
    @Operation(summary = "Manually trigger a test OTP email (dev/test only)")
    public ResponseEntity<String> sendTestEmail(@RequestBody EmailEvent event) {
        log.info("[NOTIFICATION] Manual email trigger → to={}, purpose={}", event.getTo(), event.getPurpose());
        emailService.sendOtpEmail(event);
        return ResponseEntity.ok("Test email dispatched to: " + event.getTo());
    }
}
