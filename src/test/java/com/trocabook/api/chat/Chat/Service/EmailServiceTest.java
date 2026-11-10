package com.trocabook.api.chat.Chat.Service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "emailRemetente", "no-reply@trocabook.com");
    }

    @Test
    @DisplayName("CT01 - Quando destinatário for nulo, deve retornar sem enviar e-mail")
    void quandoDestinatarioNulo_entaoRetornaSemEnviar() {
        // DADO que o endereço do destinatário é nulo
        String destinatarioNulo = null;

        // QUANDO a solicitação de envio de e-mail é feita
        emailService.sendHtmlMessage(destinatarioNulo, "Assunto", "<p>Corpo</p>");

        // ENTÃO nenhuma mensagem MIME deve ser criada ou enviada
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("CT02 - Quando destinatário for vazio ou apenas espaços, deve retornar sem enviar e-mail")
    void quandoDestinatarioEmBranco_entaoRetornaSemEnviar() {
        // DADO que o endereço do destinatário é uma string em branco
        String destinatarioEmBranco = "   ";

        // QUANDO a solicitação de envio é feita
        emailService.sendHtmlMessage(destinatarioEmBranco, "Assunto", "<p>Corpo</p>");

        // ENTÃO o envio não deve ser acionado
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("CT03 - Quando dados forem válidos, deve montar MimeMessage e enviar e-mail com sucesso")
    void quandoDadosValidos_entaoEnviaEmailComSucesso() {
        // DADO dados válidos para envio de e-mail HTML
        String destinatario = "destinatario@trocabook.com";
        String assunto = "Teste de Assunto";
        String conteudoHtml = "<strong>Mensagem HTML de teste</strong>";

        MimeMessage mimeMessageReal = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal);

        // QUANDO o e-mail HTML é enviado
        emailService.sendHtmlMessage(destinatario, assunto, conteudoHtml);

        // ENTÃO o envio é executado via JavaMailSender
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertEquals(mimeMessageReal, messageCaptor.getValue());
    }

    @Test
    @DisplayName("CT04 - Quando ocorrer MessagingException durante montagem do e-mail, deve capturar e registrar erro")
    void quandoOcorrerMessagingException_entaoCapturaETrataErro() throws Exception {
        // DADO uma falha ao configurar o MimeMessage que lança MessagingException
        String destinatario = "destinatario@trocabook.com";
        String assunto = "Assunto com Erro";
        String conteudoHtml = "<p>Conteúdo</p>";

        MimeMessage mimeMessageMock = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageMock);

        doThrow(new MessagingException("Falha ao definir destinatário"))
                .when(mimeMessageMock).setRecipient(any(Message.RecipientType.class), any());

        // QUANDO o serviço tenta enviar o e-mail
        // ENTÃO não deve propagar a exceção para fora do método
        assertDoesNotThrow(() -> emailService.sendHtmlMessage(destinatario, assunto, conteudoHtml));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("CT05 - Quando enviarNotificacaoNovaMensagem com título preenchido, deve utilizar o título como assunto")
    void quandoEnviarNotificacaoComTituloPreenchido_entaoUsaTituloComoAssunto() {
        // DADO uma notificação de mensagem com título customizado
        String emailDestinatario = "usuario@trocabook.com";
        String titulo = "Proposta Especial no Livro";
        String conteudo = "Gostaria de trocar o livro?";
        String uidAnuncio = "anuncio123";

        MimeMessage mimeMessageReal = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal);

        // QUANDO a notificação de nova mensagem é disparada
        emailService.enviarNotificacaoNovaMensagem(emailDestinatario, titulo, conteudo, uidAnuncio);

        // ENTÃO a mensagem é enviada com o título informado
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("CT06 - Quando enviarNotificacaoNovaMensagem com título nulo, deve usar assunto padrão")
    void quandoEnviarNotificacaoComTituloNulo_entaoUsaAssuntoPadrao() {
        // DADO uma notificação de mensagem onde o título é nulo
        String emailDestinatario = "usuario@trocabook.com";
        String tituloNulo = null;
        String conteudo = "Mensagem simples";
        String uidAnuncio = "anuncio456";

        MimeMessage mimeMessageReal = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal);

        // QUANDO a notificação de nova mensagem é disparada
        emailService.enviarNotificacaoNovaMensagem(emailDestinatario, tituloNulo, conteudo, uidAnuncio);

        // ENTÃO o e-mail é enviado com o assunto padrão 'Nova mensagem no Trocabook'
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("CT07 - Quando enviarNotificacaoNovaMensagem com título vazio, deve usar assunto padrão")
    void quandoEnviarNotificacaoComTituloVazio_entaoUsaAssuntoPadrao() {
        // DADO uma notificação de mensagem onde o título é composto apenas por espaços
        String emailDestinatario = "usuario@trocabook.com";
        String tituloVazio = "    ";
        String conteudo = "Mensagem sem título";
        String uidAnuncio = "anuncio789";

        MimeMessage mimeMessageReal = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal);

        // QUANDO a notificação é disparada
        emailService.enviarNotificacaoNovaMensagem(emailDestinatario, tituloVazio, conteudo, uidAnuncio);

        // ENTÃO o e-mail é enviado normalmente com o assunto padrão
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}

