package server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import server.service.RequestRouter;
import server.service.Resp;
import server.util.ServerLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** Uma conexão = uma requisição = uma resposta; depois a conexão é encerrada. */
public class ClientHandler implements Runnable {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private final Socket socket;
    private final RequestRouter router;

    public ClientHandler(Socket socket, RequestRouter router) {
        this.socket = socket;
        this.router = router;
    }

    @Override
    public void run() {
        String clientIp = socket.getInetAddress().getHostAddress();
        ServerLog.log("Cliente conectado: /" + clientIp);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            socket.setSoTimeout(10000);

            String inputLine = reader.readLine();
            if (inputLine == null || inputLine.trim().isEmpty()) return;

            ServerLog.log("[Servidor] Requisicao recebida: " + inputLine);

            JsonObject resp;
            try {
                resp = router.rotearRequisicao(inputLine, clientIp);
            } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | ClassCastException e) {
                ServerLog.log("[Servidor] JSON inválido recebido de " + clientIp);
                resp = Resp.criar(400, "Requisição inválida", null);
            } catch (Exception e) {
                ServerLog.log("[Servidor] Erro ao tratar requisição: " + e.getMessage());
                e.printStackTrace();
                resp = Resp.criar(500, Resp.MSG_ERRO_INTERNO, null);
            }

            String json = gson.toJson(resp);
            ServerLog.log("[Servidor] Resposta enviada: " + json);
            writer.println(json);

        } catch (IOException e) {
            ServerLog.log("[Servidor] Erro de conexão com " + clientIp + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
            ServerLog.log("Cliente desconectado: /" + clientIp);
        }
    }
}
