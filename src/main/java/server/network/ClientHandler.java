package server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import server.service.RequestRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestRouter router;
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public ClientHandler(Socket socket, RequestRouter router) {
        this.socket = socket;
        this.router = router;
    }

    @Override
    public void run() {
        String clientIp = socket.getInetAddress().getHostAddress();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            socket.setSoTimeout(10000);

            String inputLine = reader.readLine();
            if (inputLine == null || inputLine.trim().isEmpty()) return;

            JsonObject resp;
            try {
                resp = router.rotearRequisicao(inputLine, clientIp);
            } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | ClassCastException e) {
                System.out.println("[FALHA REQUISIÇÃO] JSON inválido de " + clientIp);
                resp = router.criarResposta(400, "Requisição inválida", null);
            } catch (Exception e) {
                System.err.println("[ERRO REQUISIÇÃO]: " + e.getMessage());
                e.printStackTrace();
                resp = router.criarResposta(500, "Erro interno no servidor", null);
            }

            writer.println(gson.toJson(resp));

        } catch (IOException e) {
            System.err.println("[ERRO CONEXÃO] " + clientIp + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}