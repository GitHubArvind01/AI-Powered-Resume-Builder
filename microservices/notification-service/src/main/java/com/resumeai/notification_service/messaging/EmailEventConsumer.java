package com.resumeai.notification_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.resumeai.notification_service.dto.EmailEvent;
import com.resumeai.notification_service.service.EmailService;

import lombok.RequiredArgsConstructor;

/**
 * Listens to {@code email.queue} and forwards each {@link EmailEvent}
 * to {@link EmailService} for delivery via SMTP.
 *
 * <p>This is a fire-and-forget consumer — Auth Service has already returned
 * its response to the client by the time this method runs.</p>
 */
@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailEventConsumer.class);

    private final EmailService emailService;

    /**
     * Consume an email event from RabbitMQ.
     *
     * <p>Runs on a separate thread managed by the AMQP listener container.
     * The Jackson converter (configured in {@code RabbitMQConfig}) automatically
     * deserializes the JSON message body into an {@link EmailEvent}.</p>
     *
     * @param event the deserialized email event from the broker
     */
    @RabbitListener(queues = "${app.rabbitmq.queue:email.queue}")
    public void consumeEmailEvent(EmailEvent event) {
        log.info("[NOTIFICATION] Received email event → to={}, purpose={}", event.getTo(), event.getPurpose());

        if (event.getTo() == null || event.getTo().isBlank()) {
            log.warn("[NOTIFICATION] Skipping event — recipient email is blank. purpose={}", event.getPurpose());
            return;
        }

        if (event.getOtp() == null || event.getOtp().isBlank()) {
            log.warn("[NOTIFICATION] Skipping event — OTP is blank. to={}", event.getTo());
            return;
        }

        emailService.sendOtpEmail(event);
    }
}
