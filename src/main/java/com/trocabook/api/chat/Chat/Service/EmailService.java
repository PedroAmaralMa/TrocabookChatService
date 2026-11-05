package com.trocabook.api.chat.Chat.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailRemetente;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia um e-mail com conteúdo HTML de forma genérica.
     * @param to E-mail do destinatário
     * @param subject Assunto do e-mail
     * @param htmlContent Corpo do e-mail em formato HTML
     */
    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        if (to == null || to.isBlank()) {
            log.warn("Destinatário de e-mail inválido ou em branco. Envio cancelado.");
            return;
        }

        log.info("Iniciando envio de e-mail para '{}' com assunto '{}'", to, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(emailRemetente, "Trocabook"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("E-mail enviado com sucesso para '{}'", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("Erro ao enviar e-mail HTML: " + e.getMessage());
            log.error("Erro ao enviar e-mail HTML para '{}': {}", to, e.getMessage(), e);
        }
    }

    /**
     * Método utilitário para formatar a notificação de nova mensagem do chat.
     */
    public void enviarNotificacaoNovaMensagem(String emailDestinatario, String titulo, String conteudoMensagem, String uidAnuncio) {
        log.info("Preparando notificação por e-mail para '{}' referente ao anúncio '{}'", emailDestinatario, uidAnuncio);
        String assunto = (titulo != null && !titulo.isBlank()) ? titulo : "Nova mensagem no Trocabook";

        String htmlTemplate = """
            <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                <h2>Você recebeu uma nova mensagem no Trocabook!</h2>
                <blockquote style="background: #f4f4f4; padding: 12px; border-left: 4px solid #007bff; border-radius: 4px;">
                    %s
                </blockquote>
                <p>Acesse o Trocabook para visualizar a conversa e responder.</p>
            </div>
        """.formatted(conteudoMensagem);

        sendHtmlMessage(emailDestinatario, assunto, htmlTemplate);
    }
}
