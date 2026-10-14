package client.controller;

import com.google.gson.JsonObject;
import client.model.UserSession;
import client.network.NetworkService;

public class ClientController {

    private final NetworkService networkService;
    private final UserSession session;

    public ClientController(NetworkService networkService) {
        this.networkService = networkService;
        this.session = new UserSession();
    }

    public UserSession getSession() {
        return session;
    }

    public NetworkService getNetworkService() {
        return networkService;
    }

    public JsonObject login(String username, String password) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);

        JsonObject resp = networkService.enviarRequisicao("login", data);
        int code = resp.get("statusCode").getAsInt();

        if (code == 200) {
            String token= resp.getAsJsonObject("data").get("token").getAsString();
            session.setToken(token);
            session.setUsername(username);
            carregarNomeUsuario();
        }
        return resp;
    }

    public JsonObject cadastrar(String name, String username, String password) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("username", username);
        data.addProperty("password", password);

        return networkService.enviarRequisicao("register", data);
    }

    public void carregarNomeUsuario() {
        try {
            JsonObject resp = buscarMeusDados();
            if (resp.get("statusCode").getAsInt() == 200) {
                session.setName(resp.getAsJsonObject("data").get("name").getAsString());
            }
        } catch (Exception ignored) {}
    }

    public JsonObject buscarMeusDados() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());
        data.addProperty("username", session.getUsername());

        return networkService.enviarRequisicao("getuser", data);
    }

    public JsonObject alterarSenha(String oldPass, String newPass) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());
        data.addProperty("username", session.getUsername());
        data.addProperty("oldPassword", oldPass);
        data.addProperty("newPassword", newPass);

        return networkService.enviarRequisicao("updateuserpassword", data);
    }

    public JsonObject alterarNome(String newName) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());
        data.addProperty("username", session.getUsername());
        data.addProperty("name", newName);

        JsonObject resp = networkService.enviarRequisicao("updateusername", data);
        if (resp.get("statusCode").getAsInt() == 200) {
            session.setName(newName);
        }
        return resp;
    }

    public JsonObject listarUsuariosLogados() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());

        return networkService.enviarRequisicao("listlogged", data);
    }

    public JsonObject obterStatusUsuarios() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());

        return networkService.enviarRequisicao("getuserstatus", data);
    }
    public void logout() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("token", session.getToken());
            networkService.enviarRequisicao("logout", data);
        } catch (Exception ignored) {}

        session.clear();
    }

    public JsonObject excluirConta() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());
        data.addProperty("username", session.getUsername());

        JsonObject resp = networkService.enviarRequisicao("deleteuser", data);
        if (resp.get("statusCode").getAsInt() == 200) {
            session.clear();
        }
        return resp;
    }
}