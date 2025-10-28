package com.trocabook.api.chat.Chat.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensagens_backup")
public class MensagemBackup {

    @Id
    private String id;

    private String idMensagemOriginal; // referência da mensagem
    private int cdUsuarioRemetente;
    private int cdUsuarioDestinatario;
    private int cdUsuarioLivro;

    private String conteudo;
    private LocalDateTime dataEnvio;
    private LocalDateTime dataBackup;


    private TipoBackup tipoBackup;

    public MensagemBackup() {}

    public MensagemBackup(Mensagem mensagem, TipoBackup tipoBackup) {
        this.setIdMensagemOriginal(mensagem.getId());
        this.setCdUsuarioRemetente(mensagem.getCdUsuarioRemetente());
        this.setConteudo(mensagem.getConteudo());
        this.setCdUsuarioLivro(mensagem.getCdUsuarioLivro());
        this.setDataEnvio(mensagem.getDataEnvio());
        this.setCdUsuarioDestinatario(mensagem.getCdUsuarioDestinatario());
        this.setDataBackup(LocalDateTime.now());
        this.setTipoBackup(tipoBackup);
    }

    public enum TipoBackup {
        ALTERACAO,
        EXCLUSAO
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdMensagemOriginal() {
        return idMensagemOriginal;
    }

    public void setIdMensagemOriginal(String idMensagemOriginal) {
        this.idMensagemOriginal = idMensagemOriginal;
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

    public int getCdUsuarioLivro() {
        return cdUsuarioLivro;
    }

    public void setCdUsuarioLivro(int cdUsuarioLivro) {
        this.cdUsuarioLivro = cdUsuarioLivro;
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

    public LocalDateTime getDataBackup() {
        return dataBackup;
    }

    public void setDataBackup(LocalDateTime dataExclusao) {
        this.dataBackup = dataExclusao;
    }

    public TipoBackup getTipoBackup() {
        return tipoBackup;
    }

    public void setTipoBackup(TipoBackup tipoBackup) {
        this.tipoBackup = tipoBackup;
    }
}
