package com.trocabook.api.chat.Chat.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String QUEUE_STORAGE = "chat.queue.storage";
    public static final String QUEUE_DELIVERY = "chat.queue.delivery";
    public static final String QUEUE_NOTIFICATIONS = "chat.queue.notifications";
    public static final String ROUTING_KEY_MESSAGES = "chat.message.#";
    public static final String ROUTING_KEY_DELIVERY = "chat.delivery.#";

    @Bean
    public TopicExchange chatExchange() {
        log.info("Inicializando TopicExchange: '{}'", CHAT_EXCHANGE);
        return new TopicExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Queue storageQueue() {
        log.info("Inicializando Fila RabbitMQ para persistência: '{}'", QUEUE_STORAGE);
        return new Queue(QUEUE_STORAGE, true);
    }

    @Bean
    public Queue deliveryQueue() {
        log.info("Inicializando Fila RabbitMQ para entrega em tempo real: '{}'", QUEUE_DELIVERY);
        return new Queue(QUEUE_DELIVERY, true);
    }

    @Bean
    public Queue notificationsQueue() {
        log.info("Inicializando Fila RabbitMQ para notificações: '{}'", QUEUE_NOTIFICATIONS);
        return new Queue(QUEUE_NOTIFICATIONS, true);
    }

    @Bean
    public Binding bindingStorage(Queue storageQueue, TopicExchange chatExchange) {
        log.info("Criando Binding entre fila '{}' e exchange '{}' com routingKey '{}'",
                QUEUE_STORAGE, CHAT_EXCHANGE, ROUTING_KEY_MESSAGES);
        return BindingBuilder.bind(storageQueue).to(chatExchange).with(ROUTING_KEY_MESSAGES);
    }

    @Bean
    public Binding bindingDelivery(Queue deliveryQueue, TopicExchange chatExchange) {
        log.info("Criando Binding entre fila '{}' e exchange '{}' com routingKey '{}'",
                QUEUE_DELIVERY, CHAT_EXCHANGE, ROUTING_KEY_DELIVERY);
        return BindingBuilder.bind(deliveryQueue).to(chatExchange).with(ROUTING_KEY_DELIVERY);
    }

    @Bean
    public Binding bindingNotifications(Queue notificationsQueue, TopicExchange chatExchange) {
        log.info("Criando Binding entre fila '{}' e exchange '{}' com routingKey '{}'",
                QUEUE_NOTIFICATIONS, CHAT_EXCHANGE, ROUTING_KEY_MESSAGES);
        return BindingBuilder.bind(notificationsQueue).to(chatExchange).with(ROUTING_KEY_MESSAGES);
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        log.info("Configurando Jackson2JsonMessageConverter para RabbitMQ");
        return new Jackson2JsonMessageConverter();
    }
}
