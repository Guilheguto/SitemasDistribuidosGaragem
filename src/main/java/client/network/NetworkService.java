package client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkService {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private String serverIp;
    private int serverPort;

    public NetworkService(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public JsonObject enviarRequisicao(String method, JsonObject data) throws Exception {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(serverIp, serverPort), 5000); // timeout de conexão
            socket.setSoTimeout(10000);                                        // timeout de resposta

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            JsonObject req = new JsonObject();
            req.addProperty("method", method);
            req.add("data", data);

            writer.println(gson.toJson(req));
            String responseStr = reader.readLine();

            if (responseStr == null) {
                throw new IOException("O servidor encerrou a conexão sem enviar resposta");
            }
            return gson.fromJson(responseStr, JsonObject.class);
        } finally {
            socket.close();
        }
    }

    public String getServerIp() {
        return serverIp;
    }
    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}