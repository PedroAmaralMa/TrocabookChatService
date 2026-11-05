package com.trocabook.api.chat.Chat.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    public void enviarPushNotification(String fcmToken, String titulo, String corpo, String uidAnuncio) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("Tentativa de enviar push notification com token FCM nulo ou vazio.");
            return;
        }

        String tokenMascarado = mascararToken(fcmToken);
        log.info("Solicitada notificação push via FCM. Título: '{}', Token: '{}', Anúncio: '{}'", titulo, tokenMascarado, uidAnuncio);

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(titulo)
                        .setBody(corpo)
                        .build())
                .putData("uidAnuncio", uidAnuncio)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Notificação push enviada com sucesso via FCM. MessageId: '{}', Token: '{}'", messageId, tokenMascarado);
        } catch (FirebaseMessagingException e) {
            System.err.println("Erro ao enviar Push Notification via FCM: " + e.getMessage());
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            if (errorCode != null) {
                switch (errorCode) {
                    case UNREGISTERED:
                        log.warn("FCM [UNREGISTERED]: O token do dispositivo não está mais registrado ou expirou. Token: '{}'. Causa: {}", tokenMascarado, e.getMessage());
                        break;
                    case INVALID_ARGUMENT:
                        log.warn("FCM [INVALID_ARGUMENT]: O token ou argumento fornecido é inválido. Token: '{}'. Causa: {}", tokenMascarado, e.getMessage());
                        break;
                    case THIRD_PARTY_AUTH_ERROR:
                        log.error("FCM [THIRD_PARTY_AUTH_ERROR]: Erro de autenticação com o serviço terceiro (APNs/FCM). Causa: {}", e.getMessage(), e);
                        break;
                    default:
                        log.error("FCM [{}]: Falha no envio de push notification para o token '{}': {}", errorCode, tokenMascarado, e.getMessage(), e);
                        break;
                }
            } else {
                log.error("FCM: Erro desconhecido ao enviar push notification para o token '{}': {}", tokenMascarado, e.getMessage(), e);
            }
        }
    }

    private String mascararToken(String token) {
        if (token == null) {
            return "null";
        }
        if (token.length() <= 10) {
            return token + "...";
        }
        return token.substring(0, 10) + "...";
    }
}
