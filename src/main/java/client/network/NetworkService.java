package client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/** Cada requisição abre uma nova conexão TCP, envia uma linha JSON, lê uma linha e fecha. */
public class NetworkService {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private volatile String serverIp;
    private volatile int serverPort;
    private volatile Consumer<String> logListener;

    public NetworkService(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public JsonObject enviarRequisicao(String method, JsonObject data) throws Exception {
        String ip = serverIp;
        int porta = serverPort;
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, porta), 5000); // timeout de conexão
            socket.setSoTimeout(10000);                              // timeout de resposta

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            JsonObject req = new JsonObject();
            req.addProperty("method", method);
            req.add("data", data);

            String json = gson.toJson(req);
            registrar("[CLIENTE] Mensagem enviada para " + ip + ":" + porta + " -> " + json);
            writer.println(json);

            String responseStr = reader.readLine();
            if (responseStr == null) {
                throw new IOException("O servidor encerrou a conexão sem enviar resposta");
            }
            registrar("[CLIENTE] Mensagem recebida <- " + responseStr);
            return gson.fromJson(responseStr, JsonObject.class);
        } catch (Exception e) {
            registrar("[CLIENTE] Falha de comunicação com " + ip + ":" + porta + " -> " + e.getMessage());
            throw e;
        } finally {
            socket.close();
        }
    }
    
    public void registrar(String mensagem) {
        System.out.println(mensagem);
        Consumer<String> l = logListener;
        if (l != null) l.accept(mensagem);
    }

    public void setLogListener(Consumer<String> logListener) { this.logListener = logListener; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
}
