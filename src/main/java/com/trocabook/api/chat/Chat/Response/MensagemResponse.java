package com.trocabook.api.chat.Chat.Response;

public class MensagemResponse<T>{
    private boolean resultado;
    private T dados;
    private String mensagem;

    MensagemResponse(boolean resultado, T dados, String mensagem){
        this.resultado = resultado;
        this.dados = dados;
        this.mensagem = mensagem;
    }

    public boolean isResultado() {
        return resultado;
    }

    public void setResultado(boolean resultado) {
        this.resultado = resultado;
    }

    public T getDados() {
        return dados;
    }

    public void setDados(T dados) {
        this.dados = dados;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
