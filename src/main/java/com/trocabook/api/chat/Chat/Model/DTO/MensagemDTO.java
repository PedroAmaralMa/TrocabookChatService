package com.trocabook.api.chat.Chat.Model.DTO;

import com.trocabook.api.chat.Chat.Model.Mensagem;

import java.time.LocalDateTime;

public class MensagemDTO {
    private String id;
    private String uidRemetente;
    private String uidDestinatario;
    private String uidAnuncio;
    private String conteudo;
    private LocalDateTime dataEnvio;
    public MensagemDTO(Mensagem m) {
        this.id = m.getId().toHexString();
        this.uidRemetente = m.getUidRemetente();
        this.uidDestinatario = m.getUidDestinatario();
        this.uidAnuncio = m.getUidAnuncio();
        this.conteudo = m.getConteudo();
        this.dataEnvio = m.getDataEnvio();
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public MensagemDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUidRemetente() {
        return uidRemetente;
    }

    public void setUidRemetente(String uidRemetente) {
        this.uidRemetente = uidRemetente;
    }

    public String getUidDestinatario() {
        return uidDestinatario;
    }

    public void setUidDestinatario(String uidDestinatario) {
        this.uidDestinatario = uidDestinatario;
    }

    public String getUidAnuncio() {
        return uidAnuncio;
    }

    public void setUidAnuncio(String uidAnuncio) {
        this.uidAnuncio = uidAnuncio;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

}
