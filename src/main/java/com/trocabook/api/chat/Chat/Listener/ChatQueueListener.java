package com.trocabook.api.chat.Chat.Listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trocabook.api.chat.Chat.Config.RabbitMQConfig;
import com.trocabook.api.chat.Chat.Model.DTO.EventoChatDTO;
import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Service.IChatService;
import com.trocabook.api.chat.Chat.Service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ChatQueueListener.class);

    private final IChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoService notificacaoService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ChatQueueListener(
            IChatService chatService,
            SimpMessagingTemplate messagingTemplate,
            NotificacaoService notificacaoService,
            RabbitTemplate rabbitTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.notificacaoService = notificacaoService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STORAGE)
    public void escutarParaPersistencia(MensagemDTO mensagemDTO) {
        log.info("Mensagem consumida da fila '{}' para persistência. Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                RabbitMQConfig.QUEUE_STORAGE, mensagemDTO.getUidRemetente(), mensagemDTO.getUidDestinatario(), mensagemDTO.getUidAnuncio());
        try {
            MensagemDTO mensagemSalva = chatService.enviarMensagem(mensagemDTO);
            log.info("Mensagem persistida com sucesso a partir da fila '{}'. ID gerado: '{}'",
                    RabbitMQConfig.QUEUE_STORAGE, mensagemSalva.getId());

            String u1 = mensagemSalva.getUidRemetente().compareTo(mensagemSalva.getUidDestinatario()) < 0
                    ? mensagemSalva.getUidRemetente() : mensagemSalva.getUidDestinatario();
            String u2 = mensagemSalva.getUidRemetente().compareTo(mensagemSalva.getUidDestinatario()) < 0
                    ? mensagemSalva.getUidDestinatario() : mensagemSalva.getUidRemetente();

            String routingKeyDelivery = "chat.delivery.mensagem." + mensagemSalva.getUidAnuncio() + "." + u1 + "." + u2;

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    routingKeyDelivery,
                    mensagemSalva
            );
            log.info("Mensagem salva despachada para entrega via RabbitMQ com routingKey: '{}'", routingKeyDelivery);
        } catch (Exception e) {
            log.error("Erro ao persistir mensagem da fila '{}': {}", RabbitMQConfig.QUEUE_STORAGE, e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_DELIVERY)
    public void escutarParaEntregaEmTempoReal(Object payload) {
        log.info("Mensagem/Evento consumido da fila '{}' para entrega via WebSocket.", RabbitMQConfig.QUEUE_DELIVERY);
        try {
            if (payload instanceof Map<?, ?> map) {
                if (map.containsKey("tipo")) {
                    payload = objectMapper.convertValue(map, EventoChatDTO.class);
                } else {
                    payload = objectMapper.convertValue(map, MensagemDTO.class);
                }
            }

            if (payload instanceof MensagemDTO mensagemDTO) {

                String u1 = mensagemDTO.getUidRemetente().compareTo(mensagemDTO.getUidDestinatario()) < 0
                        ? mensagemDTO.getUidRemetente() : mensagemDTO.getUidDestinatario();
                String u2 = mensagemDTO.getUidRemetente().compareTo(mensagemDTO.getUidDestinatario()) < 0
                        ? mensagemDTO.getUidDestinatario() : mensagemDTO.getUidRemetente();

                String destinoTopico = "/topic/conversa." + mensagemDTO.getUidAnuncio() + "." + u1 + "." + u2;

                EventoChatDTO evento = new EventoChatDTO(EventoChatDTO.TipoEvento.NOVA_MENSAGEM, mensagemDTO);
                messagingTemplate.convertAndSend(destinoTopico, evento);
                log.info("Nova mensagem entregue via WebSocket no tópico: '{}'", destinoTopico);

            } else if (payload instanceof EventoChatDTO eventoDTO) {
                // Novo fluxo para Edição ou Exclusão sincronizadas via RabbitMQ
                MensagemDTO mensagem = eventoDTO.getMensagem();
                if (mensagem == null) return;

                String u1 = mensagem.getUidRemetente().compareTo(mensagem.getUidDestinatario()) < 0
                        ? mensagem.getUidRemetente() : mensagem.getUidDestinatario();
                String u2 = mensagem.getUidRemetente().compareTo(mensagem.getUidDestinatario()) < 0
                        ? mensagem.getUidDestinatario() : mensagem.getUidRemetente();

                String destinoTopico = "/topic/conversa." + mensagem.getUidAnuncio() + "." + u1 + "." + u2;

                messagingTemplate.convertAndSend(destinoTopico, eventoDTO);
                log.info("Evento '{}' entregue via WebSocket no tópico: '{}'", eventoDTO.getTipo(), destinoTopico);
            }
        } catch (Exception e) {
            log.error("Erro ao entregar evento via WebSocket na fila '{}': {}", RabbitMQConfig.QUEUE_DELIVERY, e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATIONS)
    public void escutarParaNotificacoes(MensagemDTO mensagemDTO) {
        log.info("Mensagem consumida da fila '{}' para verificação de notificações. Remetente: '{}', Destinatário: '{}'",
                RabbitMQConfig.QUEUE_NOTIFICATIONS, mensagemDTO.getUidRemetente(), mensagemDTO.getUidDestinatario());
        try {
            notificacaoService.processarNotificacao(mensagemDTO);
            log.info("Processamento de notificação concluído para mensagem da fila '{}'", RabbitMQConfig.QUEUE_NOTIFICATIONS);
        } catch (Exception e) {
            log.error("Erro ao processar notificação a partir da fila '{}': {}", RabbitMQConfig.QUEUE_NOTIFICATIONS, e.getMessage(), e);
            throw e;
        }
    }
}
