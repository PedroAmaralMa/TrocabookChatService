package com.trocabook.api.chat.Chat.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registrando endpoint STOMP: '/ws-chat' com suporte a SockJS e origens liberadas (*)");
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        log.info("Configurando MessageBroker: prefixos de aplicação '/app', simpleBroker '/topic', '/queue' e destino de usuário '/user'");
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Configurando interceptor de canal de entrada do cliente WebSocket para autenticação STOMP");
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String uidUsuario = accessor.getFirstNativeHeader("uidUsuario");
                    if (uidUsuario != null && !uidUsuario.isBlank()) {
                        log.info("Sessão WebSocket conectada: associando Principal ao uidUsuario '{}'", uidUsuario);
                        // Atribui o uidUsuario ao Principal da sessão WebSocket
                        accessor.setUser(() -> uidUsuario);
                    } else {
                        log.warn("Sessão WebSocket conectada sem o cabeçalho 'uidUsuario'.");
                    }
                }
                return message;
            }
        });
    }
}
