package com.trocabook.api.chat.Chat.Listener;

import com.trocabook.api.chat.Chat.Config.RabbitMQConfig;
import com.trocabook.api.chat.Chat.Model.DTO.EventoChatDTO;
import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Service.IChatService;
import com.trocabook.api.chat.Chat.Service.NotificacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ChatQueueListener.class)
class ChatQueueListenerTest {

    @Autowired
    private ChatQueueListener chatQueueListener;

    @MockitoBean
    private IChatService chatService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private NotificacaoService notificacaoService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("CT01 - Quando escutar fila de persistência com dados válidos, deve salvar mensagem e encaminhar para exchange de entrega")
    void quandoEscutarParaPersistenciaComDadosValidos_entaoPersisteERepassaParaExchange() {
        // DADO que uma mensagem válida foi consumida da fila de armazenamento (QUEUE_STORAGE)
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setUidRemetente("user1");
        mensagemEntrada.setUidDestinatario("user2");
        mensagemEntrada.setUidAnuncio("anuncio123");
        mensagemEntrada.setConteudo("Olá, livro disponível?");

        MensagemDTO mensagemSalva = new MensagemDTO();
        mensagemSalva.setId("64a5f2e1bcf123456789abcd");
        mensagemSalva.setUidRemetente("user1");
        mensagemSalva.setUidDestinatario("user2");
        mensagemSalva.setUidAnuncio("anuncio123");
        mensagemSalva.setConteudo("Olá, livro disponível?");
        mensagemSalva.setDataEnvio(LocalDateTime.now());

        String routingKeyEsperada = "chat.delivery.mensagem.anuncio123.user1.user2";

        when(chatService.enviarMensagem(mensagemEntrada)).thenReturn(mensagemSalva);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // QUANDO o listener consome a mensagem para persistência
        chatQueueListener.escutarParaPersistencia(mensagemEntrada);

        // ENTÃO o serviço de persistência é acionado e a mensagem resultante é despachada para a exchange de entrega
        verify(chatService, times(1)).enviarMensagem(mensagemEntrada);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.CHAT_EXCHANGE),
                eq(routingKeyEsperada),
                eq(mensagemSalva)
        );
    }

    @Test
    @DisplayName("CT02 - Quando ocorrer erro ao persistir mensagem no chatService, deve capturar no catch e relançar exceção")
    void quandoEscutarParaPersistenciaComErroNoChatService_entaoCapturaRelancaExcecaoENaoPublica() {
        // DADO que o serviço de chat lança uma exceção durante o salvamento da mensagem
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setUidRemetente("user1");
        mensagemEntrada.setUidDestinatario("user2");
        mensagemEntrada.setUidAnuncio("anuncio123");
        mensagemEntrada.setConteudo("Falha de conexão com banco");

        when(chatService.enviarMensagem(mensagemEntrada))
                .thenThrow(new RuntimeException("Falha de conexão com MongoDB"));

        try {
            // QUANDO o listener tenta processar a persistência
            chatQueueListener.escutarParaPersistencia(mensagemEntrada);
            // ENTÃO não deve alcançar esta linha
            fail("Deveria ter lançado RuntimeException devido à falha de conexão com MongoDB");
        } catch (RuntimeException e) {
            // ENTÃO a exceção capturada no bloco catch deve conter a mensagem esperada e a republicação não deve ocorrer
            assertEquals("Falha de conexão com MongoDB", e.getMessage());
        }

        verify(chatService, times(1)).enviarMensagem(mensagemEntrada);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("CT03 - Quando ocorrer erro ao despachar para RabbitMQ após persistência, deve capturar no catch e relançar exceção")
    void quandoEscutarParaPersistenciaComErroNoRabbitTemplate_entaoCapturaERelancaExcecao() {
        // DADO que a mensagem é persistida com sucesso mas o RabbitTemplate falha ao despachar
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setUidRemetente("user1");
        mensagemEntrada.setUidDestinatario("user2");
        mensagemEntrada.setUidAnuncio("anuncio123");

        MensagemDTO mensagemSalva = new MensagemDTO();
        mensagemSalva.setId("64a5f2e1bcf123456789abcd");
        mensagemSalva.setUidRemetente("user1");
        mensagemSalva.setUidDestinatario("user2");
        mensagemSalva.setUidAnuncio("anuncio123");

        when(chatService.enviarMensagem(mensagemEntrada)).thenReturn(mensagemSalva);
        doThrow(new RuntimeException("Erro ao enviar mensagem para RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.CHAT_EXCHANGE), anyString(), eq(mensagemSalva));

        try {
            // QUANDO o listener consome a mensagem e tenta encaminhar
            chatQueueListener.escutarParaPersistencia(mensagemEntrada);
            // ENTÃO deve falhar se não lançar exceção
            fail("Deveria ter lançado RuntimeException ao falhar o envio para RabbitMQ");
        } catch (RuntimeException e) {
            // ENTÃO validação robusta da mensagem capturada no bloco catch
            assertEquals("Erro ao enviar mensagem para RabbitMQ", e.getMessage());
        }

        verify(chatService, times(1)).enviarMensagem(mensagemEntrada);
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.CHAT_EXCHANGE), anyString(), eq(mensagemSalva));
    }

    @Test
    @DisplayName("CT04 - Quando escutar fila de entrega em tempo real (remetente < destinatário com MensagemDTO), deve montar tópico ordenado e enviar via WebSocket")
    void quandoEscutarParaEntregaEmTempoRealComRemetenteMenorQueDestinatario_entaoEnviaParaWebSocketComTopicoOrdenado() {
        // DADO que uma mensagem é consumida com remetente lexicograficamente menor que o destinatário
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setId("msg123");
        mensagemEntrada.setUidRemetente("userA");
        mensagemEntrada.setUidDestinatario("userB");
        mensagemEntrada.setUidAnuncio("anuncio55");
        mensagemEntrada.setConteudo("Mensagem tempo real");

        String topicoEsperado = "/topic/conversa.anuncio55.userA.userB";
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // QUANDO o listener processa a entrega em tempo real
        chatQueueListener.escutarParaEntregaEmTempoReal(mensagemEntrada);

        // ENTÃO o evento WebSocket de tipo NOVA_MENSAGEM é enviado para o tópico devidamente ordenado
        ArgumentCaptor<EventoChatDTO> captorEvento = ArgumentCaptor.forClass(EventoChatDTO.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq(topicoEsperado), captorEvento.capture());

        EventoChatDTO eventoEnviado = captorEvento.getValue();
        assertNotNull(eventoEnviado);
        assertEquals(EventoChatDTO.TipoEvento.NOVA_MENSAGEM, eventoEnviado.getTipo());
        assertEquals(mensagemEntrada, eventoEnviado.getMensagem());
    }

    @Test
    @DisplayName("CT05 - Quando escutar fila de entrega em tempo real (remetente > destinatário com MensagemDTO), deve inverter ordem dos usuários no tópico WebSocket")
    void quandoEscutarParaEntregaEmTempoRealComRemetenteMaiorQueDestinatario_entaoInverteOrdemNoTopicoWebSocket() {
        // DADO que uma mensagem é consumida com remetente lexicograficamente maior que o destinatário
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setId("msg456");
        mensagemEntrada.setUidRemetente("userZ");
        mensagemEntrada.setUidDestinatario("userA");
        mensagemEntrada.setUidAnuncio("anuncio77");
        mensagemEntrada.setConteudo("Mensagem invertida");

        String topicoEsperado = "/topic/conversa.anuncio77.userA.userZ";
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // QUANDO o listener processa a entrega em tempo real
        chatQueueListener.escutarParaEntregaEmTempoReal(mensagemEntrada);

        // ENTÃO o tópico deve garantir u1=userA e u2=userZ
        verify(messagingTemplate, times(1)).convertAndSend(eq(topicoEsperado), any(EventoChatDTO.class));
    }

    @Test
    @DisplayName("CT06 - Quando escutar fila de entrega em tempo real com EventoChatDTO, deve despachar evento no tópico ordenado")
    void quandoEscutarParaEntregaEmTempoRealComEventoChatDTO_entaoDespachaEventoNoTopicoOrdenado() {
        // DADO que um EventoChatDTO de edição/exclusão é consumido da fila de entrega
        MensagemDTO msg = new MensagemDTO();
        msg.setId("msg789");
        msg.setUidRemetente("userB");
        msg.setUidDestinatario("userA");
        msg.setUidAnuncio("anuncio88");

        EventoChatDTO eventoDTO = new EventoChatDTO(EventoChatDTO.TipoEvento.MENSAGEM_EDITADA, msg);

        String topicoEsperado = "/topic/conversa.anuncio88.userA.userB";
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // QUANDO o listener processa a entrega em tempo real
        chatQueueListener.escutarParaEntregaEmTempoReal(eventoDTO);

        // ENTÃO o evento WebSocket correspondente é entregue no tópico ordenado
        verify(messagingTemplate, times(1)).convertAndSend(eq(topicoEsperado), eq(eventoDTO));
    }

    @Test
    @DisplayName("CT07 - Quando ocorrer erro ao enviar via WebSocket na entrega em tempo real, deve capturar no catch e relançar exceção")
    void quandoEscutarParaEntregaEmTempoRealComErroNoWebSocket_entaoCapturaERelancaExcecao() {
        // DADO que ocorre um erro ao disparar a mensagem via SimpMessagingTemplate
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setUidRemetente("userA");
        mensagemEntrada.setUidDestinatario("userB");
        mensagemEntrada.setUidAnuncio("anuncio55");

        doThrow(new MessagingException("Falha na conexão com cliente STOMP"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        try {
            // QUANDO o listener processa a entrega em tempo real
            chatQueueListener.escutarParaEntregaEmTempoReal(mensagemEntrada);
            // ENTÃO não deve alcançar esta linha
            fail("Deveria ter lançado MessagingException devido à falha no broker WebSocket");
        } catch (MessagingException e) {
            // ENTÃO validação da mensagem capturada no bloco catch
            assertEquals("Falha na conexão com cliente STOMP", e.getMessage());
        }

        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("CT08 - Quando escutar fila de notificações com dados válidos, deve acionar NotificacaoService com sucesso")
    void quandoEscutarParaNotificacoesComDadosValidos_entaoProcessaNotificacaoComSucesso() {
        // DADO uma mensagem consumida da fila de notificações (QUEUE_NOTIFICATIONS)
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setUidRemetente("user1");
        mensagemEntrada.setUidDestinatario("user2");
        mensagemEntrada.setUidAnuncio("anuncio99");
        mensagemEntrada.setConteudo("Você tem uma nova mensagem!");

        doNothing().when(notificacaoService).processarNotificacao(mensagemEntrada);

        // QUANDO o listener consome a mensagem para notificação
        chatQueueListener.escutarParaNotificacoes(mensagemEntrada);

        // ENTÃO o serviço de notificação deve ser chamado exatamente 1 vez com a mensagem consumida
        verify(notificacaoService, times(1)).processarNotificacao(mensagemEntrada);
    }

    @Test
    @DisplayName("CT09 - Quando ocorrer erro ao processar notificação no NotificacaoService, deve capturar no catch e relançar exceção")
    void quandoEscutarParaNotificacoesComErroNoServico_entaoCapturaERelancaExcecao() {
        // DADO que o NotificacaoService lança uma exceção durante o processamento
        MensagemDTO mensagemEntrada = new MensagemDTO();
        mensagemEntrada.setUidRemetente("user1");
        mensagemEntrada.setUidDestinatario("user2");
        mensagemEntrada.setUidAnuncio("anuncio99");

        doThrow(new RuntimeException("Falha ao comunicar com FCM ou SMTP"))
                .when(notificacaoService).processarNotificacao(mensagemEntrada);

        try {
            // QUANDO o listener processa a notificação
            chatQueueListener.escutarParaNotificacoes(mensagemEntrada);
            // ENTÃO não deve alcançar esta linha
            fail("Deveria ter lançado RuntimeException devido à falha no serviço de notificação");
        } catch (RuntimeException e) {
            // ENTÃO validação robusta da mensagem capturada no bloco catch
            assertEquals("Falha ao comunicar com FCM ou SMTP", e.getMessage());
        }

        verify(notificacaoService, times(1)).processarNotificacao(mensagemEntrada);
    }

    @Test
    @DisplayName("CT10 - Quando escutar fila de entrega em tempo real com Map (LinkedHashMap), deve converter para MensagemDTO e entregar")
    void quandoEscutarParaEntregaEmTempoRealComMap_entaoConverteParaMensagemDTOEEntrega() {
        // DADO que o RabbitListener entrega o payload como Map (comportamento padrão quando desserializa Object)
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", "msgMap123");
        map.put("uidRemetente", "userA");
        map.put("uidDestinatario", "userB");
        map.put("uidAnuncio", "anuncio99");
        map.put("conteudo", "Olá via Map");

        String topicoEsperado = "/topic/conversa.anuncio99.userA.userB";
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // QUANDO o listener consome o payload Map
        chatQueueListener.escutarParaEntregaEmTempoReal(map);

        // ENTÃO a mensagem deve ser convertida para MensagemDTO e despachada para o WebSocket
        verify(messagingTemplate, times(1)).convertAndSend(eq(topicoEsperado), any(EventoChatDTO.class));
    }
}
