package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.DTO.UsuarioStatusNotificacaoDTO;
import com.trocabook.api.chat.Chat.Model.UsuarioNotificacao;
import com.trocabook.api.chat.Chat.Repository.UsuarioNotificacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final SimpUserRegistry userRegistry;
    private final FcmPushService fcmPushService;
    private final EmailService emailService;
    private final UsuarioNotificacaoRepository usuarioNotificacaoRepository;

    @Autowired
    public NotificacaoService(
            SimpUserRegistry userRegistry,
            FcmPushService fcmPushService,
            EmailService emailService,
            UsuarioNotificacaoRepository usuarioNotificacaoRepository) {
        this.userRegistry = userRegistry;
        this.fcmPushService = fcmPushService;
        this.emailService = emailService;
        this.usuarioNotificacaoRepository = usuarioNotificacaoRepository;
    }

    public void salvarStatusUsuario(UsuarioStatusNotificacaoDTO dto) {
        log.info("Salvando/atualizando status de notificação do usuário: uid='{}', email='{}'", dto.getUidUsuario(), dto.getEmail());
        UsuarioNotificacao usuario = usuarioNotificacaoRepository.findByUidUsuario(dto.getUidUsuario())
                .orElse(new UsuarioNotificacao());

        usuario.setId(dto.getUidUsuario());
        usuario.setUidUsuario(dto.getUidUsuario());
        usuario.setEmail(dto.getEmail());
        usuario.setFcmToken(dto.getFcmToken());
        usuario.setUltimaAtualizacao(LocalDateTime.now());

        usuarioNotificacaoRepository.save(usuario);
        log.info("Status de notificação salvo com sucesso no banco para o usuário '{}'", dto.getUidUsuario());
    }

    public void processarNotificacao(MensagemDTO mensagemDTO) {
        String uidDestinatario = mensagemDTO.getUidDestinatario();
        String uidAnuncio = mensagemDTO.getUidAnuncio();

        log.info("Processando verificação de notificação. Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                mensagemDTO.getUidRemetente(), uidDestinatario, uidAnuncio);

        String u1 = mensagemDTO.getUidRemetente().compareTo(uidDestinatario) < 0 ? mensagemDTO.getUidRemetente() : uidDestinatario;
        String u2 = mensagemDTO.getUidRemetente().compareTo(uidDestinatario) < 0 ? uidDestinatario : mensagemDTO.getUidRemetente();
        String canalConversa = "/topic/conversa." + uidAnuncio + "." + u1 + "." + u2;

        boolean usuarioComChatAberto = estaInscritoNoCanal(uidDestinatario, canalConversa);

        if (usuarioComChatAberto) {
            log.info("Usuário destinatário '{}' já está com o chat aberto no canal '{}'. Notificação push/e-mail dispensada.", uidDestinatario, canalConversa);
            return;
        }

        Optional<UsuarioNotificacao> usuarioOpt = usuarioNotificacaoRepository.findByUidUsuario(uidDestinatario);
        if (usuarioOpt.isEmpty()) {
            log.warn("Nenhum registro de notificação encontrado para o destinatário '{}'. Envio cancelado.", uidDestinatario);
            return;
        }

        UsuarioNotificacao usuario = usuarioOpt.get();

        if (usuario.getFcmToken() != null && !usuario.getFcmToken().isBlank()) {
            log.info("Engatilhando envio de push notification via FCM para o destinatário '{}'", uidDestinatario);
            fcmPushService.enviarPushNotification(
                    usuario.getFcmToken(),
                    "Nova mensagem no Trocabook",
                    mensagemDTO.getConteudo(),
                    uidAnuncio
            );
        } else if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            log.info("FCM token ausente. Engatilhando envio de notificação por e-mail para '{}' (usuário '{}')", usuario.getEmail(), uidDestinatario);
            emailService.enviarNotificacaoNovaMensagem(
                    usuario.getEmail(),
                    "Nova mensagem no Trocabook",
                    mensagemDTO.getConteudo(),
                    uidAnuncio
            );
        } else {
            log.warn("Destinatário '{}' não possui nem FCM token nem e-mail configurados para receber notificações.", uidDestinatario);
        }
    }

    private boolean estaInscritoNoCanal(String uidUsuario, String canal) {
        var user = userRegistry.getUser(uidUsuario);
        if (user == null) {
            log.debug("Usuário '{}' não encontrado no registro WebSocket.", uidUsuario);
            return false;
        }
        boolean inscrito = user.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream())
                .anyMatch(sub -> sub.getDestination().equals(canal));
        log.debug("Verificação de inscrição WebSocket: usuário='{}', canal='{}', inscrito={}", uidUsuario, canal, inscrito);
        return inscrito;
    }
}
