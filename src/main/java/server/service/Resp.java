package server.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/** Monta respostas no formato padrão: { statusCode, message, data }. */
public final class Resp {

    public static final String MSG_SESSAO_EXPIRADA = "Sessão expirada ou encerrada.";
    public static final String MSG_TOKEN_AUSENTE = "Token de autenticação não fornecido.";
    public static final String MSG_CAMPOS_INVALIDOS = "Campos obrigatórios ausentes ou inválidos.";
    public static final String MSG_ERRO_INTERNO = "Erro interno no servidor.";

    private Resp() {}

    public static JsonObject criar(int statusCode, String message, JsonElement data) {
        JsonObject resp = new JsonObject();
        resp.addProperty("statusCode", statusCode);
        resp.addProperty("message", message);
        resp.add("data", data == null ? JsonNull.INSTANCE : data);
        return resp;
    }

    public static RespostaException camposInvalidos() {
        return new RespostaException(400, MSG_CAMPOS_INVALIDOS);
    }
}
