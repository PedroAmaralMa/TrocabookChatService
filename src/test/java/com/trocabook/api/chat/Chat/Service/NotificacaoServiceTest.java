package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.DTO.UsuarioStatusNotificacaoDTO;
import com.trocabook.api.chat.Chat.Model.UsuarioNotificacao;
import com.trocabook.api.chat.Chat.Repository.UsuarioNotificacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private SimpUserRegistry userRegistry;

    @Mock
    private FcmPushService fcmPushService;

    @Mock
    private EmailService emailService;

    @Mock
    private UsuarioNotificacaoRepository usuarioNotificacaoRepository;

    private NotificacaoService notificacaoService;

    @BeforeEach
    void setUp() {
        notificacaoService = new NotificacaoService(
                userRegistry,
                fcmPushService,
                emailService,
                usuarioNotificacaoRepository
        );
    }

    @Test
    @DisplayName("CT01 - Quando salvar status de usuário já existente no banco, deve atualizar e salvar")
    void quandoSalvarStatusUsuarioExistente_entaoAtualizaDados() {
        // DADO um usuário que já possui registro prévio no repositório
        UsuarioStatusNotificacaoDTO dto = new UsuarioStatusNotificacaoDTO();
        dto.setUidUsuario("user123");
        dto.setEmail("novo-email@trocabook.com");
        dto.setFcmToken("novo-fcm-token");

        UsuarioNotificacao usuarioExistente = new UsuarioNotificacao();
        usuarioExistente.setId("user123");
        usuarioExistente.setUidUsuario("user123");
        usuarioExistente.setEmail("antigo-email@trocabook.com");
        usuarioExistente.setFcmToken("antigo-token");

        when(usuarioNotificacaoRepository.findByUidUsuario("user123")).thenReturn(Optional.of(usuarioExistente));

        // QUANDO o serviço salva o status do usuário
        notificacaoService.salvarStatusUsuario(dto);

        // ENTÃO o usuário existente é atualizado com as novas informações
        ArgumentCaptor<UsuarioNotificacao> captor = ArgumentCaptor.forClass(UsuarioNotificacao.class);
        verify(usuarioNotificacaoRepository, times(1)).save(captor.capture());

        UsuarioNotificacao salvo = captor.getValue();
        assertEquals("user123", salvo.getUidUsuario());
        assertEquals("novo-email@trocabook.com", salvo.getEmail());
        assertEquals("novo-fcm-token", salvo.getFcmToken());
        assertNotNull(salvo.getUltimaAtualizacao());
    }

    @Test
    @DisplayName("CT02 - Quando salvar status de novo usuário (inexistente no banco), deve criar e salvar novo registro")
    void quandoSalvarStatusNovoUsuario_entaoCriaNovoRegistro() {
        // DADO que não existe registro prévio para o usuário
        UsuarioStatusNotificacaoDTO dto = new UsuarioStatusNotificacaoDTO();
        dto.setUidUsuario("novoUser");
        dto.setEmail("novo@trocabook.com");
        dto.setFcmToken("tokenNovo123");

        when(usuarioNotificacaoRepository.findByUidUsuario("novoUser")).thenReturn(Optional.empty());

        // QUANDO o serviço salva o status
        notificacaoService.salvarStatusUsuario(dto);

        // ENTÃO uma nova entidade é criada e persistida com os dados informados
        ArgumentCaptor<UsuarioNotificacao> captor = ArgumentCaptor.forClass(UsuarioNotificacao.class);
        verify(usuarioNotificacaoRepository, times(1)).save(captor.capture());

        UsuarioNotificacao salvo = captor.getValue();
        assertEquals("novoUser", salvo.getId());
        assertEquals("novoUser", salvo.getUidUsuario());
        assertEquals("novo@trocabook.com", salvo.getEmail());
        assertEquals("tokenNovo123", salvo.getFcmToken());
    }

    @Test
    @DisplayName("CT03 - Quando usuário destinatário estiver com chat aberto no canal WebSocket, não deve enviar notificação")
    void quandoUsuarioDestinatarioComChatAberto_entaoNaoEnviaNotificacao() {
        // DADO uma mensagem enviada para um destinatário que já está com a sessão aberta no canal da conversa
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userA");
        mensagemDTO.setUidDestinatario("userB");
        mensagemDTO.setUidAnuncio("anuncio1");
        mensagemDTO.setConteudo("Oi, tudo bem?");

        String canalEsperado = "/topic/conversa.anuncio1.userA.userB";

        SimpUser simpUser = mock(SimpUser.class);
        SimpSession session = mock(SimpSession.class);
        SimpSubscription subscription = mock(SimpSubscription.class);

        when(userRegistry.getUser("userB")).thenReturn(simpUser);
        when(simpUser.getSessions()).thenReturn(Set.of(session));
        when(session.getSubscriptions()).thenReturn(Set.of(subscription));
        when(subscription.getDestination()).thenReturn(canalEsperado);

        // QUANDO a notificação é processada
        notificacaoService.processarNotificacao(mensagemDTO);

        // ENTÃO nenhuma notificação deve ser enviada por push ou e-mail
        verify(usuarioNotificacaoRepository, never()).findByUidUsuario(any());
        verify(fcmPushService, never()).enviarPushNotification(any(), any(), any(), any());
        verify(emailService, never()).enviarNotificacaoNovaMensagem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CT04 - Quando usuário destinatário não possuir cadastro de notificação, deve abortar sem enviar")
    void quandoDestinatarioNaoCadastradoParaNotificacao_entaoAbortaEnvio() {
        // DADO que o usuário destinatário não está no canal e não possui registro na tabela de notificações
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userA");
        mensagemDTO.setUidDestinatario("userB");
        mensagemDTO.setUidAnuncio("anuncio1");
        mensagemDTO.setConteudo("Teste");

        when(userRegistry.getUser("userB")).thenReturn(null);
        when(usuarioNotificacaoRepository.findByUidUsuario("userB")).thenReturn(Optional.empty());

        // QUANDO o processamento de notificação é disparado
        notificacaoService.processarNotificacao(mensagemDTO);

        // ENTÃO nenhum envio ocorre
        verify(fcmPushService, never()).enviarPushNotification(any(), any(), any(), any());
        verify(emailService, never()).enviarNotificacaoNovaMensagem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CT05 - Quando destinatário possuir FCM token válido, deve enviar Push Notification")
    void quandoDestinatarioPossuiFcmToken_entaoEnviaPushNotification() {
        // DADO que o usuário destinatário possui token FCM cadastrado
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userA");
        mensagemDTO.setUidDestinatario("userB");
        mensagemDTO.setUidAnuncio("anuncio1");
        mensagemDTO.setConteudo("Olá, livro disponível?");

        UsuarioNotificacao usuario = new UsuarioNotificacao();
        usuario.setUidUsuario("userB");
        usuario.setFcmToken("fcm-token-valido-12345");
        usuario.setEmail("userB@trocabook.com");

        when(userRegistry.getUser("userB")).thenReturn(null);
        when(usuarioNotificacaoRepository.findByUidUsuario("userB")).thenReturn(Optional.of(usuario));

        // QUANDO a notificação é processada
        notificacaoService.processarNotificacao(mensagemDTO);

        // ENTÃO deve disparar a push notification via FCM e não enviar e-mail
        verify(fcmPushService, times(1)).enviarPushNotification(
                eq("fcm-token-valido-12345"),
                eq("Nova mensagem no Trocabook"),
                eq("Olá, livro disponível?"),
                eq("anuncio1")
        );
        verify(emailService, never()).enviarNotificacaoNovaMensagem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CT06 - Quando destinatário não tiver FCM token mas tiver e-mail, deve enviar notificação por e-mail")
    void quandoDestinatarioNaoPossuiFcmTokenMasPossuiEmail_entaoEnviaEmail() {
        // DADO que o usuário destinatário não tem token FCM (nulo ou vazio) mas possui e-mail cadastrado
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userA");
        mensagemDTO.setUidDestinatario("userB");
        mensagemDTO.setUidAnuncio("anuncio1");
        mensagemDTO.setConteudo("Proposta de troca enviada");

        UsuarioNotificacao usuario = new UsuarioNotificacao();
        usuario.setUidUsuario("userB");
        usuario.setFcmToken("");
        usuario.setEmail("userB@trocabook.com");

        when(userRegistry.getUser("userB")).thenReturn(null);
        when(usuarioNotificacaoRepository.findByUidUsuario("userB")).thenReturn(Optional.of(usuario));

        // QUANDO a notificação é processada
        notificacaoService.processarNotificacao(mensagemDTO);

        // ENTÃO deve enviar e-mail de notificação e não disparar push notification
        verify(emailService, times(1)).enviarNotificacaoNovaMensagem(
                eq("userB@trocabook.com"),
                eq("Nova mensagem no Trocabook"),
                eq("Proposta de troca enviada"),
                eq("anuncio1")
        );
        verify(fcmPushService, never()).enviarPushNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CT07 - Quando destinatário não tiver nem FCM token nem e-mail, não deve disparar nenhum envio")
    void quandoDestinatarioNaoPossuiFcmTokenNemEmail_entaoNaoEnviaNada() {
        // DADO que o usuário destinatário tem registro mas sem token FCM e sem e-mail
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userA");
        mensagemDTO.setUidDestinatario("userB");
        mensagemDTO.setUidAnuncio("anuncio1");
        mensagemDTO.setConteudo("Mensagem");

        UsuarioNotificacao usuario = new UsuarioNotificacao();
        usuario.setUidUsuario("userB");
        usuario.setFcmToken(null);
        usuario.setEmail(null);

        when(userRegistry.getUser("userB")).thenReturn(null);
        when(usuarioNotificacaoRepository.findByUidUsuario("userB")).thenReturn(Optional.of(usuario));

        // QUANDO a notificação é processada
        notificacaoService.processarNotificacao(mensagemDTO);

        // ENTÃO nenhuma chamada de push ou e-mail é efetuada
        verify(fcmPushService, never()).enviarPushNotification(any(), any(), any(), any());
        verify(emailService, never()).enviarNotificacaoNovaMensagem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CT08 - Quando remetente for lexicograficamente maior que destinatário, deve ordenar canal corretamente e prosseguir")
    void quandoRemetenteMaiorQueDestinatario_entaoOrdenaCanalEProcessa() {
        // DADO que o remetente é maior que o destinatário e o usuário está inscrito em canal diferente
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userZ");
        mensagemDTO.setUidDestinatario("userA");
        mensagemDTO.setUidAnuncio("anuncio99");
        mensagemDTO.setConteudo("Mensagem invertida");

        SimpUser simpUser = mock(SimpUser.class);
        SimpSession session = mock(SimpSession.class);
        SimpSubscription subscription = mock(SimpSubscription.class);

        // O usuário userA está conectado mas inscrito em outro tópico diferente
        when(userRegistry.getUser("userA")).thenReturn(simpUser);
        when(simpUser.getSessions()).thenReturn(Set.of(session));
        when(session.getSubscriptions()).thenReturn(Set.of(subscription));
        when(subscription.getDestination()).thenReturn("/topic/outro.canal");

        UsuarioNotificacao usuario = new UsuarioNotificacao();
        usuario.setUidUsuario("userA");
        usuario.setFcmToken("token-valido-userA");

        when(usuarioNotificacaoRepository.findByUidUsuario("userA")).thenReturn(Optional.of(usuario));

        // QUANDO a notificação é processada
        notificacaoService.processarNotificacao(mensagemDTO);

        // ENTÃO como o canal de conversa ordenado é /topic/conversa.anuncio99.userA.userZ, a notificação deve ser enviada via FCM
        verify(fcmPushService, times(1)).enviarPushNotification(
                eq("token-valido-userA"),
                eq("Nova mensagem no Trocabook"),
                eq("Mensagem invertida"),
                eq("anuncio99")
        );
    }
}

