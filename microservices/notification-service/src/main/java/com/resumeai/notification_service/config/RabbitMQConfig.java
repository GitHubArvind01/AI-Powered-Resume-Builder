package com.resumeai.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure beans for Notification Service.
 * <p>
 * Declares the same exchange / queue / routing-key as Auth Service so that
 * both sides are always in sync.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "email.exchange";
    public static final String QUEUE       = "email.queue";
    public static final String ROUTING_KEY = "email.routing";

    // -------------------------------------------------------------------------
    // Infrastructure beans
    // -------------------------------------------------------------------------

    /** Durable queue — survives broker restarts */
    @Bean
    public Queue emailQueue() {
        return new Queue(QUEUE, true);
    }

    /** Direct exchange */
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EXCHANGE);
    }

    /** Bind queue to exchange with routing key */
    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue).to(emailExchange).with(ROUTING_KEY);
    }

    // -------------------------------------------------------------------------
    // JSON serialisation
    // -------------------------------------------------------------------------

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** Wire the JSON converter into outbound template (for future use) */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }

    /**
     * Ensure @RabbitListener containers use JSON deserialization.
     * Without this, Spring defaults to Java serialization and the consumer
     * cannot deserialize the incoming EmailEvent JSON.
     */
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter());
        return factory;
    }
}
