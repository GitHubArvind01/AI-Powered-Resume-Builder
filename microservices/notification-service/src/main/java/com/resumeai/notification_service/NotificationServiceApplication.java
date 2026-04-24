package com.resumeai.notification_service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service — consumes email events from RabbitMQ and delivers
 * them via SMTP. Part of the AI-Powered Resume Builder microservices system.
 *
 * <p>Event flow:
 * Auth Service → RabbitMQ (email.exchange / email.routing) → email.queue
 *             → {@code EmailEventConsumer} → {@code EmailServiceImpl} → Gmail SMTP
 * </p>
 */
@SpringBootApplication
@EnableRabbit
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
