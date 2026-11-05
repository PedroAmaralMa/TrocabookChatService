package com.trocabook.api.chat.Chat.Model.DTO;

public class UsuarioStatusNotificacaoDTO {
    private String uidUsuario;
    private String email;
    private String fcmToken;

    public UsuarioStatusNotificacaoDTO() {}

    public UsuarioStatusNotificacaoDTO(String uidUsuario, String email, String fcmToken) {
        this.uidUsuario = uidUsuario;
        this.email = email;
        this.fcmToken = fcmToken;
    }

    public String getUidUsuario() { return uidUsuario; }
    public void setUidUsuario(String uidUsuario) { this.uidUsuario = uidUsuario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}