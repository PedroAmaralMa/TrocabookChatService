package com.trocabook.api.chat.Chat.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensagens")
public class Mensagem {
    @Id
    private String id;

    private int cdUsuarioRemetente;
    private int cdUsuarioDestinatario;
    private int cdUsuarioLivro;
    private String conteudo;
    private LocalDateTime dataEnvio;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCdUsuarioRemetente() {
        return cdUsuarioRemetente;
    }

    public void setCdUsuarioRemetente(int cdUsuarioRemetente) {
        this.cdUsuarioRemetente = cdUsuarioRemetente;
    }

    public int getCdUsuarioDestinatario() {
        return cdUsuarioDestinatario;
    }

    public void setCdUsuarioDestinatario(int cdUsuarioDestinatario) {
        this.cdUsuarioDestinatario = cdUsuarioDestinatario;
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

    public int getCdUsuarioLivro() {
        return cdUsuarioLivro;
    }

    public void setCdUsuarioLivro(int cdUsuarioLivro) {
        this.cdUsuarioLivro = cdUsuarioLivro;
    }
}
