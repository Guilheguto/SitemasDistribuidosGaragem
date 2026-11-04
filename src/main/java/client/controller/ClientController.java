package client.controller;

import client.model.UserSession;
import client.network.NetworkService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Uma função por método do protocolo. Todas devolvem a resposta bruta do servidor. */
public class ClientController {

    private final NetworkService networkService;
    private final UserSession session;

    public ClientController(NetworkService networkService) {
        this.networkService = networkService;
        this.session = new UserSession();
    }

    public UserSession getSession() { return session; }
    public NetworkService getNetworkService() { return networkService; }

    private static int codigo(JsonObject resp) {
        if (resp == null) return -1;
        JsonElement el = resp.get("statusCode");
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsInt() : -1;
    }

    private JsonObject comToken() {
        JsonObject data = new JsonObject();
        data.addProperty("token", session.getToken());
        return data;
    }

    // ------------------------ acesso ------------------------

    public JsonObject login(String username, String password) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);

        JsonObject resp = networkService.enviarRequisicao("login", data);
        if (codigo(resp) == 200) {
            session.setToken(resp.getAsJsonObject("data").get("token").getAsString());
            session.setUsername(username);
            session.setRole("user");
            carregarNomeUsuario(); // nome e role vêm do servidor
            networkService.registrar("[CLIENTE] Login realizado: '" + username + "' (" + session.getRole() + ")");
        }
        return resp;
    }

    public void logout() {
        if (session.getToken() != null) {
            try {
                networkService.enviarRequisicao("logout", comToken());
                networkService.registrar("[CLIENTE] Logout realizado: '" + session.getUsername() + "'");
            } catch (Exception ignored) {}
        }
        session.clear();
    }

    // ------------------------ usuários ------------------------

    /** Cadastro público (role = user). */
    public JsonObject cadastrar(String name, String username, String password) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("username", username);
        data.addProperty("password", password);
        return networkService.enviarRequisicao("register", data);
    }

    /** Cadastro feito por um admin, com role (user/admin) e token. */
    public JsonObject cadastrarComRole(String name, String username, String password, String role) throws Exception {
        JsonObject data = comToken();
        data.addProperty("name", name);
        data.addProperty("username", username);
        data.addProperty("password", password);
        data.addProperty("role", role);
        return networkService.enviarRequisicao("register", data);
    }

    public void carregarNomeUsuario() {
        try {
            JsonObject resp = buscarMeusDados();
            if (codigo(resp) == 200) {
                JsonObject d = resp.getAsJsonObject("data");
                session.setName(d.get("name").getAsString());
                JsonElement role = d.get("role");
                if (role != null && role.isJsonPrimitive()) session.setRole(role.getAsString());
            }
        } catch (Exception ignored) {}
    }

    public JsonObject buscarMeusDados() throws Exception {
        return buscarUsuario(session.getUsername());
    }

    /** getuser: para admin retorna o usuário pedido; para user, os próprios dados. Não altera a sessão. */
    public JsonObject buscarUsuario(String username) throws Exception {
        JsonObject data = comToken();
        data.addProperty("username", username);
        return networkService.enviarRequisicao("getuser", data);
    }

    public JsonObject alterarSenha(String oldPass, String newPass) throws Exception {
        JsonObject data = comToken();
        data.addProperty("username", session.getUsername());
        data.addProperty("oldPassword", oldPass);
        data.addProperty("newPassword", newPass);
        return networkService.enviarRequisicao("updateuserpassword", data);
    }

    public JsonObject alterarNome(String newName) throws Exception {
        return alterarNomeDeUsuario(session.getUsername(), newName);
    }

    /** updateusername: admin pode alterar qualquer usuário; a sessão só muda se for a própria conta. */
    public JsonObject alterarNomeDeUsuario(String username, String newName) throws Exception {
        JsonObject data = comToken();
        data.addProperty("username", username);
        data.addProperty("name", newName);

        JsonObject resp = networkService.enviarRequisicao("updateusername", data);
        if (codigo(resp) == 200 && username.equals(session.getUsername())) {
            session.setName(newName);
        }
        return resp;
    }

    public JsonObject excluirConta() throws Exception {
        return excluirUsuario(session.getUsername());
    }

    /** deleteuser: a sessão só é limpa se a conta excluída for a do próprio usuário logado. */
    public JsonObject excluirUsuario(String username) throws Exception {
        JsonObject data = comToken();
        data.addProperty("username", username);

        JsonObject resp = networkService.enviarRequisicao("deleteuser", data);
        if (codigo(resp) == 200 && username.equals(session.getUsername())) {
            session.clear();
        }
        return resp;
    }

    // ------------------------ extras do nosso servidor (fora do protocolo) ------------------------

    public JsonObject listarUsuariosLogados() throws Exception {
        return networkService.enviarRequisicao("listlogged", comToken());
    }

    public JsonObject obterStatusUsuarios() throws Exception {
        return networkService.enviarRequisicao("getuserstatus", comToken());
    }

    // ------------------------ vagas ------------------------

    public JsonObject criarVaga(String spotName, int floor, String type) throws Exception {
        JsonObject data = comToken();
        data.addProperty("floor", floor);
        data.addProperty("spotName", spotName);
        data.addProperty("type", type);
        return networkService.enviarRequisicao("createvaga", data);
    }

    public JsonObject listarVagas() throws Exception {
        return networkService.enviarRequisicao("getvagas", comToken());
    }

    /** O chamador monta apenas os campos que quer alterar (spotName + opcionais); o token é acrescentado aqui. */
    public JsonObject atualizarVaga(JsonObject campos) throws Exception {
        JsonObject data = comToken();
        for (java.util.Map.Entry<String, JsonElement> e : campos.entrySet()) {
            data.add(e.getKey(), e.getValue());
        }
        return networkService.enviarRequisicao("updatevaga", data);
    }

    public JsonObject removerVaga(String spotName) throws Exception {
        JsonObject data = comToken();
        data.addProperty("spotName", spotName);
        return networkService.enviarRequisicao("deletevaga", data);
    }

    // ------------------------ veículos ------------------------

    public JsonObject criarVeiculo(String plate, String brand, String model, String type) throws Exception {
        JsonObject data = comToken();
        data.addProperty("brand", brand);
        data.addProperty("model", model); // null vira JSON null
        data.addProperty("type", type);
        data.addProperty("licensePlate", plate);
        return networkService.enviarRequisicao("createvehicle", data);
    }

    public JsonObject listarVeiculos() throws Exception {
        return networkService.enviarRequisicao("getvehicles", comToken());
    }

    public JsonObject buscarVeiculo(String plate) throws Exception {
        JsonObject data = comToken();
        data.addProperty("licensePlate", plate);
        return networkService.enviarRequisicao("getvehicle", data);
    }

    public JsonObject atualizarVeiculo(String plate, String brand, String model, String type) throws Exception {
        JsonObject data = comToken();
        data.addProperty("licensePlate", plate);
        data.addProperty("brand", brand);
        data.addProperty("model", model);
        data.addProperty("type", type);
        return networkService.enviarRequisicao("updatevehicle", data);
    }

    public JsonObject removerVeiculo(String plate) throws Exception {
        JsonObject data = comToken();
        data.addProperty("licensePlate", plate);
        return networkService.enviarRequisicao("deletevehicle", data);
    }

    public JsonObject localizarVeiculo(String plate) throws Exception {
        JsonObject data = comToken();
        data.addProperty("licensePlate", plate);
        return networkService.enviarRequisicao("findvehicle", data);
    }
}
