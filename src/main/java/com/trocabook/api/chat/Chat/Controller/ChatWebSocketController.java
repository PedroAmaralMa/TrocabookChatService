package com.trocabook.api.chat.Chat.Controller;

import com.trocabook.api.chat.Chat.Config.RabbitMQConfig;
import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public ChatWebSocketController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @MessageMapping("/chat.enviarMensagem")
    public void processarMensagemWebSocket(@Payload MensagemDTO mensagemDTO) {
        log.info("Mensagem recebida via WebSocket (/app/chat.enviarMensagem). Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                mensagemDTO.getUidRemetente(), mensagemDTO.getUidDestinatario(), mensagemDTO.getUidAnuncio());

        try {
            String u1 = mensagemDTO.getUidRemetente().compareTo(mensagemDTO.getUidDestinatario()) < 0
                    ? mensagemDTO.getUidRemetente() : mensagemDTO.getUidDestinatario();
            String u2 = mensagemDTO.getUidRemetente().compareTo(mensagemDTO.getUidDestinatario()) < 0
                    ? mensagemDTO.getUidDestinatario() : mensagemDTO.getUidRemetente();

            String routingKey = "chat.message." + mensagemDTO.getUidAnuncio() + "." + u1 + "." + u2;

            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, mensagemDTO);
            log.info("Mensagem do WebSocket despachada com sucesso para o RabbitMQ com routingKey: '{}'", routingKey);
        } catch (Exception e) {
            log.error("Erro ao despachar mensagem recebida via WebSocket para o RabbitMQ: {}", e.getMessage(), e);
            throw e;
        }
    }
}
