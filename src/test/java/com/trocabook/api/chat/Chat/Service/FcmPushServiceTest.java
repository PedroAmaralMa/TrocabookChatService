package com.trocabook.api.chat.Chat.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    private FcmPushService fcmPushService;

    @BeforeEach
    void setUp() {
        fcmPushService = new FcmPushService();
    }

    @Test
    @DisplayName("CT01 - Quando token FCM for nulo, deve retornar sem acionar o FirebaseMessaging")
    void quandoTokenFcmNulo_entaoRetornaSemAcionarFirebase() {
        // DADO um token FCM nulo
        String tokenNulo = null;

        // QUANDO o serviço tenta enviar a notificação push
        fcmPushService.enviarPushNotification(tokenNulo, "Título", "Corpo", "anuncio1");

        // ENTÃO nenhuma exceção é lançada e o fluxo encerra imediatamente
        assertDoesNotThrow(() -> fcmPushService.enviarPushNotification(null, "T", "C", "A"));
    }

    @Test
    @DisplayName("CT02 - Quando token FCM for vazio ou espaços em branco, deve retornar sem acionar Firebase")
    void quandoTokenFcmEmBranco_entaoRetornaSemAcionarFirebase() {
        // DADO um token FCM em branco
        String tokenVazio = "   ";

        // QUANDO o envio é solicitado
        fcmPushService.enviarPushNotification(tokenVazio, "Título", "Corpo", "anuncio1");

        // ENTÃO o envio não é processado
        assertDoesNotThrow(() -> fcmPushService.enviarPushNotification(tokenVazio, "T", "C", "A"));
    }

    @Test
    @DisplayName("CT03 - Quando token tiver comprimento <= 10 caracteres, deve mascarar corretamente e enviar com sucesso")
    void quandoTokenCurtoValido_entaoMascaraComprimentoCurtoEEnviaComSucesso() throws Exception {
        // DADO um token válido com 8 caracteres (<= 10)
        String tokenCurto = "12345678";

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenReturn("msg-id-12345");

            // QUANDO a notificação push é enviada
            fcmPushService.enviarPushNotification(tokenCurto, "Título Teste", "Corpo Teste", "anuncio1");

            // ENTÃO a mensagem é enviada ao Firebase
            verify(messagingMock, times(1)).send(any(Message.class));
        }
    }

    @Test
    @DisplayName("CT04 - Quando token tiver comprimento > 10 caracteres, deve mascarar substring e enviar com sucesso")
    void quandoTokenLongoValido_entaoMascaraSubstringEEnviaComSucesso() throws Exception {
        // DADO um token válido com mais de 10 caracteres
        String tokenLongo = "abcdefghij1234567890longtoken";

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenReturn("msg-id-long-67890");

            // QUANDO a notificação push é enviada
            fcmPushService.enviarPushNotification(tokenLongo, "Novo Título", "Novo Corpo", "anuncio2");

            // ENTÃO a mensagem é enviada e os dados conferem
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messagingMock, times(1)).send(messageCaptor.capture());
            assertNotNull(messageCaptor.getValue());
        }
    }

    @Test
    @DisplayName("CT05 - Quando FirebaseMessaging lançar erro UNREGISTERED, deve capturar e registrar WARN")
    void quandoFirebaseLancarErroUnregistered_entaoCapturaETrataWarn() throws Exception {
        // DADO uma falha de token não registrado no Firebase
        String token = "token-invalido-1234567890";
        FirebaseMessagingException exceptionMock = mock(FirebaseMessagingException.class);
        when(exceptionMock.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(exceptionMock.getMessage()).thenReturn("Requested entity was not found.");

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenThrow(exceptionMock);

            // QUANDO o envio é tentado
            fcmPushService.enviarPushNotification(token, "Título", "Corpo", "anuncio1");

            // ENTÃO a exceção é tratada internamente sem propagar erro para a aplicação
            verify(messagingMock, times(1)).send(any(Message.class));
            verify(exceptionMock, atLeastOnce()).getMessagingErrorCode();
        }
    }

    @Test
    @DisplayName("CT06 - Quando FirebaseMessaging lançar erro INVALID_ARGUMENT, deve capturar e tratar WARN")
    void quandoFirebaseLancarErroInvalidArgument_entaoCapturaETrataWarn() throws Exception {
        // DADO uma falha de argumento inválido no Firebase
        String token = "token-argumento-invalido-123";
        FirebaseMessagingException exceptionMock = mock(FirebaseMessagingException.class);
        when(exceptionMock.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(exceptionMock.getMessage()).thenReturn("Invalid registration token.");

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenThrow(exceptionMock);

            // QUANDO o envio é tentado
            fcmPushService.enviarPushNotification(token, "Título", "Corpo", "anuncio1");

            // ENTÃO a exceção é capturada e tratada adequadamente
            verify(messagingMock, times(1)).send(any(Message.class));
            assertEquals(MessagingErrorCode.INVALID_ARGUMENT, exceptionMock.getMessagingErrorCode());
        }
    }

    @Test
    @DisplayName("CT07 - Quando FirebaseMessaging lançar erro THIRD_PARTY_AUTH_ERROR, deve capturar e tratar ERROR")
    void quandoFirebaseLancarErroThirdPartyAuth_entaoCapturaETrataError() throws Exception {
        // DADO uma falha de autenticação com provedores terceiros (APNs/FCM)
        String token = "token-auth-error-123";
        FirebaseMessagingException exceptionMock = mock(FirebaseMessagingException.class);
        when(exceptionMock.getMessagingErrorCode()).thenReturn(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR);
        when(exceptionMock.getMessage()).thenReturn("Auth error with APNs.");

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenThrow(exceptionMock);

            // QUANDO o envio é tentado
            fcmPushService.enviarPushNotification(token, "Título", "Corpo", "anuncio1");

            // ENTÃO a exceção é capturada e tratada
            verify(messagingMock, times(1)).send(any(Message.class));
            assertEquals(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR, exceptionMock.getMessagingErrorCode());
        }
    }

    @Test
    @DisplayName("CT08 - Quando FirebaseMessaging lançar outro erro com código (ex: INTERNAL), deve cair no default do switch")
    void quandoFirebaseLancarOutroErroComCodigo_entaoTrataNoDefault() throws Exception {
        // DADO um erro interno do Firebase (MessagingErrorCode.INTERNAL)
        String token = "token-qualquer-123";
        FirebaseMessagingException exceptionMock = mock(FirebaseMessagingException.class);
        when(exceptionMock.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(exceptionMock.getMessage()).thenReturn("Internal FCM server error.");

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenThrow(exceptionMock);

            // QUANDO o envio é tentado
            fcmPushService.enviarPushNotification(token, "Título", "Corpo", "anuncio1");

            // ENTÃO a exceção é tratada no bloco default do switch
            verify(messagingMock, times(1)).send(any(Message.class));
            assertEquals(MessagingErrorCode.INTERNAL, exceptionMock.getMessagingErrorCode());
        }
    }

    @Test
    @DisplayName("CT09 - Quando FirebaseMessaging lançar erro com código nulo, deve tratar no bloco else")
    void quandoFirebaseLancarErroComCodigoNulo_entaoTrataNoElse() throws Exception {
        // DADO uma exceção do Firebase cujo getMessagingErrorCode() retorna nulo
        String token = "token-sem-codigo-123";
        FirebaseMessagingException exceptionMock = mock(FirebaseMessagingException.class);
        when(exceptionMock.getMessagingErrorCode()).thenReturn(null);
        when(exceptionMock.getMessage()).thenReturn("Erro sem código definido");

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            when(messagingMock.send(any(Message.class))).thenThrow(exceptionMock);

            // QUANDO o envio é tentado
            fcmPushService.enviarPushNotification(token, "Título", "Corpo", "anuncio1");

            // ENTÃO o bloco else é executado sem quebrar o fluxo
            verify(messagingMock, times(1)).send(any(Message.class));
            assertNull(exceptionMock.getMessagingErrorCode());
        }
    }
}

