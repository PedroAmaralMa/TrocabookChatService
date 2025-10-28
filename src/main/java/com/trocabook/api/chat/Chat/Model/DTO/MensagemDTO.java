package com.trocabook.api.chat.Chat.Model.DTO;

import com.trocabook.api.chat.Chat.Model.Mensagem;

public class MensagemDTO {
    private String id;
    private int cdUsuarioRemetente;
    private int cdUsuarioDestinatario;
    private int cdUsuarioLivro;
    private String conteudo;
    public MensagemDTO(Mensagem m) {
        this.id = m.getId().toHexString();
        this.cdUsuarioRemetente = m.getCdUsuarioRemetente();
        this.cdUsuarioDestinatario = m.getCdUsuarioDestinatario();
        this.cdUsuarioLivro = m.getCdUsuarioLivro();
        this.conteudo = m.getConteudo();
    }

    public MensagemDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCdUsuarioRemetente() {
        return cdUsuarioRemetente;
    }

    public void setCdUsuarioRemetente(int cdUsuarioRemente) {
        this.cdUsuarioRemetente = cdUsuarioRemente;
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

    public int getCdUsuarioLivro() {
        return cdUsuarioLivro;
    }

    public void setCdUsuarioLivro(int cdUsuarioLivro) {
        this.cdUsuarioLivro = cdUsuarioLivro;
    }
}
