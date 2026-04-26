package com.resumeai.notification_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.resumeai.notification_service.dto.EmailEvent;
import com.resumeai.notification_service.service.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailEventConsumer.class);

    private final EmailService emailService;

    @RabbitListener(queues = "${app.rabbitmq.queue:email.queue}")
    public void consumeEmailEvent(EmailEvent event) {
        log.info("[NOTIFICATION] Received email event to={}, purpose={}", event.getTo(), event.getPurpose());

        if (event.getTo() == null || event.getTo().isBlank()) {
            log.warn("[NOTIFICATION] Skipping event because recipient email is blank. purpose={}", event.getPurpose());
            return;
        }

        if (requiresOtp(event) && (event.getOtp() == null || event.getOtp().isBlank())) {
            log.warn("[NOTIFICATION] Skipping event because OTP is blank. to={}", event.getTo());
            return;
        }

        emailService.sendOtpEmail(event);
    }

    private boolean requiresOtp(EmailEvent event) {
        if (event == null || event.getPurpose() == null) {
            return true;
        }

        return !("USER_DELETED".equalsIgnoreCase(event.getPurpose())
                || "USER_DEACTIVATED".equalsIgnoreCase(event.getPurpose()));
    }
}
