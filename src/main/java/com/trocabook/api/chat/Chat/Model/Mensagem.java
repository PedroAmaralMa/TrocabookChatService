package com.trocabook.api.chat.Chat.Model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensagens")
public class Mensagem {
    @Id
    private ObjectId id;

    private String uidRemetente;
    private String uidDestinatario;
    private String uidAnuncio;
    private String conteudo;
    private LocalDateTime dataEnvio;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
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

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

}
