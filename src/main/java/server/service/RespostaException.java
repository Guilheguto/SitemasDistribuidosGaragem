package server.service;

import com.google.gson.JsonObject;

/** Interrompe o tratamento de uma requisição devolvendo uma resposta de erro ao cliente. */
public class RespostaException extends Exception {
    private final int statusCode;

    public RespostaException(int statusCode, String mensagem) {
        super(mensagem);
        this.statusCode = statusCode;
    }

    public JsonObject paraResposta() {
        return Resp.criar(statusCode, getMessage(), null);
    }
}
