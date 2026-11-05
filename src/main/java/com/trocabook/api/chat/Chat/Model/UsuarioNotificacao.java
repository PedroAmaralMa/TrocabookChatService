package com.trocabook.api.chat.Chat.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "usuarios_notificacao")
public class UsuarioNotificacao {

    @Id
    private String id;
    private String uidUsuario;
    private String email;
    private String fcmToken;
    private LocalDateTime ultimaAtualizacao;

    public UsuarioNotificacao() {}

    public UsuarioNotificacao(String uidUsuario, String email, String fcmToken) {
        this.id = uidUsuario;
        this.uidUsuario = uidUsuario;
        this.email = email;
        this.fcmToken = fcmToken;
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUidUsuario() { return uidUsuario; }
    public void setUidUsuario(String uidUsuario) { this.uidUsuario = uidUsuario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }
}