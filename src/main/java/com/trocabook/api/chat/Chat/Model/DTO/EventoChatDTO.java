package com.trocabook.api.chat.Chat.Model.DTO;

public class EventoChatDTO {

    public enum TipoEvento {
        NOVA_MENSAGEM,
        MENSAGEM_EDITADA,
        MENSAGEM_EXCLUIDA
    }

    private TipoEvento tipo;
    private MensagemDTO mensagem;
    private String idMensagemExcluida;

    public EventoChatDTO() {}

    public EventoChatDTO(TipoEvento tipo, MensagemDTO mensagem) {
        this.tipo = tipo;
        this.mensagem = mensagem;
    }

    public EventoChatDTO(TipoEvento tipo, String idMensagemExcluida) {
        this.tipo = tipo;
        this.idMensagemExcluida = idMensagemExcluida;
    }

    public TipoEvento getTipo() { return tipo; }
    public void setTipo(TipoEvento tipo) { this.tipo = tipo; }
    public MensagemDTO getMensagem() { return mensagem; }
    public void setMensagem(MensagemDTO mensagem) { this.mensagem = mensagem; }
    public String getIdMensagemExcluida() { return idMensagemExcluida; }
    public void setIdMensagemExcluida(String idMensagemExcluida) { this.idMensagemExcluida = idMensagemExcluida; }
}