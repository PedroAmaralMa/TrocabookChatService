package com.trocabook.api.chat.Chat.Listener;

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

@Component
public class ChatQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ChatQueueListener.class);

    private final IChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoService notificacaoService;
    private final RabbitTemplate rabbitTemplate;

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
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STORAGE)
    public void escutarParaPersistencia(MensagemDTO mensagemDTO) {
        log.info("Mensagem consumida da fila '{}' para persistência. Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                RabbitMQConfig.QUEUE_STORAGE, mensagemDTO.getUidRemetente(), mensagemDTO.getUidDestinatario(), mensagemDTO.getUidAnuncio());
        try {
            MensagemDTO mensagemSalva = chatService.enviarMensagem(mensagemDTO);
            log.info("Mensagem persistida com sucesso a partir da fila '{}'. ID gerado: '{}'",
                    RabbitMQConfig.QUEUE_STORAGE, mensagemSalva.getId());


            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    "chat.delivery.websocket",
                    mensagemSalva
            );
        } catch (Exception e) {
            log.error("Erro ao persistir mensagem da fila '{}': {}", RabbitMQConfig.QUEUE_STORAGE, e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_DELIVERY)
    public void escutarParaEntregaEmTempoReal(MensagemDTO mensagemDTO) {
        log.info("Mensagem consumida da fila '{}' para entrega via WebSocket. Remetente: '{}', Destinatário: '{}'",
                RabbitMQConfig.QUEUE_DELIVERY, mensagemDTO.getUidRemetente(), mensagemDTO.getUidDestinatario());
        try {
            String u1 = mensagemDTO.getUidRemetente().compareTo(mensagemDTO.getUidDestinatario()) < 0
                    ? mensagemDTO.getUidRemetente() : mensagemDTO.getUidDestinatario();
            String u2 = mensagemDTO.getUidRemetente().compareTo(mensagemDTO.getUidDestinatario()) < 0
                    ? mensagemDTO.getUidDestinatario() : mensagemDTO.getUidRemetente();

            String destinoTopico = "/topic/conversa." + mensagemDTO.getUidAnuncio() + "." + u1 + "." + u2;

            EventoChatDTO evento = new EventoChatDTO(EventoChatDTO.TipoEvento.NOVA_MENSAGEM, mensagemDTO);
            messagingTemplate.convertAndSend(destinoTopico, evento);
            log.info("Mensagem entregue via WebSocket com sucesso no tópico: '{}'", destinoTopico);
        } catch (Exception e) {
            log.error("Erro ao entregar mensagem via WebSocket na fila '{}': {}", RabbitMQConfig.QUEUE_DELIVERY, e.getMessage(), e);
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
