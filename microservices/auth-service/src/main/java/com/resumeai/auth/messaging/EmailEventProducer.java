package com.resumeai.auth.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.resumeai.auth.config.RabbitMQConfig;
import com.resumeai.auth.dtos.EmailEvent;

import lombok.RequiredArgsConstructor;

/**
 * Publishes {@link EmailEvent} messages to the RabbitMQ email exchange.
 * <p>
 * This is a fire-and-forget operation — Auth Service does NOT wait for the
 * email to be sent, keeping API response times fast.
 */
@Component
@RequiredArgsConstructor
public class EmailEventProducer {

    private static final Logger log = LoggerFactory.getLogger(EmailEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes an email event to the broker.
     *
     * @param event the email event containing recipient, OTP, subject and purpose
     */
    public void publishEmailEvent(EmailEvent event) {
        log.info("[AUTH] Publishing email event → to={}, purpose={}", event.getTo(), event.getPurpose());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.info("[AUTH] Email event published successfully for purpose={}", event.getPurpose());
        } catch (Exception ex) {
            // Log and continue — do NOT block the auth response
            log.error("[AUTH] Failed to publish email event for purpose={}: {}", event.getPurpose(), ex.getMessage(), ex);
        }
    }
}
