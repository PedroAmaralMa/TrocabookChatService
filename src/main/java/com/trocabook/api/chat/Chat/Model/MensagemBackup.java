package com.trocabook.api.chat.Chat.Model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensagens_backup")
public class MensagemBackup {

    @Id
    private ObjectId id;

    private ObjectId idMensagemOriginal; // referência da mensagem
    private String uidRemetente;
    private String uidDestinatario;
    private String uidAnuncio;

    private String conteudo;
    private LocalDateTime dataEnvio;
    private LocalDateTime dataBackup;


    private TipoBackup tipoBackup;

    public MensagemBackup() {}

    public MensagemBackup(Mensagem mensagem, TipoBackup tipoBackup) {
        this.setIdMensagemOriginal(mensagem.getId());
        this.setUidRemetente(mensagem.getUidRemetente());
        this.setConteudo(mensagem.getConteudo());
        this.setUidAnuncio(mensagem.getUidAnuncio());
        this.setDataEnvio(mensagem.getDataEnvio());
        this.setUidDestinatario(mensagem.getUidDestinatario());
        this.setDataBackup(LocalDateTime.now());
        this.setTipoBackup(tipoBackup);
    }

    public enum TipoBackup {
        ALTERACAO,
        EXCLUSAO
    }


    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getIdMensagemOriginal() {
        return idMensagemOriginal;
    }

    public void setIdMensagemOriginal(ObjectId idMensagemOriginal) {
        this.idMensagemOriginal = idMensagemOriginal;
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
