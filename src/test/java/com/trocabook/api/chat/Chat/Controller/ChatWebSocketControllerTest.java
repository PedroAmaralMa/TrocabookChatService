package com.trocabook.api.chat.Chat.Controller;

import com.trocabook.api.chat.Chat.Config.RabbitMQConfig;
import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ChatWebSocketController webSocketController;

    @BeforeEach
    void setUp() {
        webSocketController = new ChatWebSocketController(rabbitTemplate);
    }

    @Test
    @DisplayName("CT01 - Quando processar mensagem via WebSocket com remetente menor que destinatário, deve despachar para RabbitMQ")
    void quandoProcessarMensagemComRemetenteMenorQueDestinatario_entaoDespachaParaRabbitMQ() {
        // DADO que uma mensagem chega via WebSocket com remetente lexicograficamente menor
        MensagemDTO dto = new MensagemDTO();
        dto.setUidRemetente("userA");
        dto.setUidDestinatario("userB");
        dto.setUidAnuncio("anuncio99");
        dto.setConteudo("Mensagem WebSocket");

        String expectedRoutingKey = "chat.message.anuncio99.userA.userB";

        // QUANDO a mensagem é processada pelo controller WebSocket
        webSocketController.processarMensagemWebSocket(dto);

        // ENTÃO deve despachar para o exchange configurado com a routing key ordenada
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.CHAT_EXCHANGE), eq(expectedRoutingKey), eq(dto));
    }

    @Test
    @DisplayName("CT02 - Quando processar mensagem via WebSocket com remetente maior que destinatário, deve inverter ordem na routing key")
    void quandoProcessarMensagemComRemetenteMaiorQueDestinatario_entaoInverteOrdemNaRoutingKey() {
        // DADO que uma mensagem chega via WebSocket com remetente lexicograficamente maior
        MensagemDTO dto = new MensagemDTO();
        dto.setUidRemetente("userZ");
        dto.setUidDestinatario("userA");
        dto.setUidAnuncio("anuncio99");
        dto.setConteudo("Mensagem WebSocket Invertida");

        String expectedRoutingKey = "chat.message.anuncio99.userA.userZ";

        // QUANDO a mensagem é processada pelo controller WebSocket
        webSocketController.processarMensagemWebSocket(dto);

        // ENTÃO a routing key deve ter u1=userA e u2=userZ
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.CHAT_EXCHANGE), eq(expectedRoutingKey), eq(dto));
    }

    @Test
    @DisplayName("CT03 - Quando ocorrer erro ao despachar mensagem WebSocket para o RabbitMQ, deve relançar a exceção")
    void quandoProcessarMensagemComErroNoRabbitMQ_entaoRelancaExcecao() {
        // DADO que o RabbitMQ lança uma exceção durante o envio
        MensagemDTO dto = new MensagemDTO();
        dto.setUidRemetente("userA");
        dto.setUidDestinatario("userB");
        dto.setUidAnuncio("anuncio99");

        doThrow(new RuntimeException("Falha na conexão RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // QUANDO o processamento falha
        try {
            webSocketController.processarMensagemWebSocket(dto);
            // ENTÃO não deve alcançar este ponto
            fail("Deveria ter lançado RuntimeException");
        } catch (RuntimeException e) {
            // ENTÃO validação da mensagem da exceção lançada
            assertEquals("Falha na conexão RabbitMQ", e.getMessage());
        }

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), eq(dto));
    }
}

