package com.trocabook.api.chat.Chat.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String QUEUE_STORAGE = "chat.queue.storage";
    public static final String QUEUE_DELIVERY = "chat.queue.delivery";
    public static final String QUEUE_NOTIFICATIONS = "chat.queue.notifications";
    public static final String ROUTING_KEY_MESSAGES = "chat.message.#";
    public static final String ROUTING_KEY_DELIVERY = "chat.delivery.#";

    // --- Declaração de Topologia ---

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public Queue storageQueue() {
        return new Queue(QUEUE_STORAGE, true);
    }

    @Bean
    public Queue deliveryQueue() {
        return new Queue(QUEUE_DELIVERY, true);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(QUEUE_NOTIFICATIONS, true);
    }

    @Bean
    public Binding bindingStorage(Queue storageQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(storageQueue).to(chatExchange).with(ROUTING_KEY_MESSAGES);
    }

    @Bean
    public Binding bindingDelivery(Queue deliveryQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(deliveryQueue).to(chatExchange).with(ROUTING_KEY_DELIVERY);
    }

    @Bean
    public Binding bindingNotifications(Queue notificationsQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(notificationsQueue).to(chatExchange).with(ROUTING_KEY_MESSAGES);
    }

    // --- Gerenciador de Topologia (Cria filas/bindings no CloudAMQP) ---

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    // --- Conversor JSON Robusto (com suporte a Java 8 Date/Time) ---

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.trocabook.api.chat.*", "java.util.*");
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    // --- RabbitTemplate Configurado com o Conversor JSON ---

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        // Habilita detecção de mensagens não entregues (Unroutable)
        template.setMandatory(true);
        template.setReturnsCallback(returned -> 
            log.error("MENSAGEM NÃO ROTEADA! Exchange: {}, RoutingKey: {}, ReplyCode: {}, ReplyText: {}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText())
        );
        return template;
    }

    // --- Fábrica do Container de Escuta (@RabbitListener) ---

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // Evita loop infinito se a mensagem for inválida
        return factory;
    }
}