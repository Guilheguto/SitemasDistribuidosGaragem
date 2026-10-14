package client.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class ResponseHandler {

    private static final int STATUS_INVALIDO = -1;

    private ResponseHandler() {}

    public static boolean processarResposta(Component parent, JsonObject resp, String successMessage) {
        return processarResposta(parent, resp, successMessage, null);
    }

    public static boolean processarResposta(Component parent, JsonObject resp, String successMessage, Runnable onUnauthorized) {
        int statusCode = lerStatusCode(resp);
        if (statusCode == STATUS_INVALIDO) {
            mostrarMensagem(parent, "Resposta inválida do servidor.", "Erro de Comunicação", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String message = lerMensagem(resp);

        if (statusCode >= 200 && statusCode < 300) {
            if (successMessage != null && !successMessage.isEmpty()) {
                mostrarMensagem(parent, successMessage, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
            return true;
        }

        if (statusCode >= 400 && statusCode < 500) {
            mostrarMensagem(parent, message, mensagemErroCliente(statusCode), statusCode == 401
                    ? JOptionPane.ERROR_MESSAGE
                    : JOptionPane.WARNING_MESSAGE);
            if (statusCode == 401 && onUnauthorized != null) {
                onUnauthorized.run();
            }
            return false;
        }

        mostrarMensagem(parent, "Erro interno no servidor: " + message, "Erro do Servidor (" + statusCode + ")", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private static String mensagemErroCliente(int statusCode) {
        switch (statusCode) {
            case 400: return "Dados Inválidos (400)";
            case 401: return "Acesso Não Autorizado (401)";
            case 403: return "Acesso Negado (403)";
            case 404: return "Não Encontrado (404)";
            case 409: return "Conflito de Dados (409)";
            default:  return "Erro na Requisição (" + statusCode + ")";
        }
    }

    private static int lerStatusCode(JsonObject resp) {
        if (resp == null) return STATUS_INVALIDO;
        JsonElement elemento = resp.get("statusCode");
        if (elemento == null || !elemento.isJsonPrimitive() || !elemento.getAsJsonPrimitive().isNumber()) return STATUS_INVALIDO;
        return elemento.getAsInt();
    }

    private static String lerMensagem(JsonObject resp) {
        JsonElement elemento = resp.get("message");
        if (elemento == null || !elemento.isJsonPrimitive()) return "Erro desconhecido";
        return elemento.getAsString();
    }

    private static void mostrarMensagem(Component parent, String message, String title, int type) {
        JOptionPane.showMessageDialog(parent, message, title, type);
    }
}