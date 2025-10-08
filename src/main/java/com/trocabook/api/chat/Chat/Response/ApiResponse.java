package com.trocabook.api.chat.Chat.Response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status;
    private T data;
    private String message;

    // Construtor para sucesso
    // O generics <T> permite que essa classe seja utilizada para qualquer tipo de
    // dado
    public ApiResponse(T data, String message) {
        this.status = "success";
        this.data = data;
        this.message = message;
    }

    // Construtor para erro
    public ApiResponse(String message) {
        this.status = "error";
        this.data = null; // Para erros, a parte de dados pode ser nula
        this.message = message;
    }

    public ApiResponse() {

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}