package server.service;

import com.google.gson.*;
import server.database.SpotDao;
import server.database.SqlUtil;
import server.database.UserDao;
import server.model.User;
import server.util.ServerLog;

import java.sql.SQLException;

/**
 * Recebe a linha JSON, identifica o "method" e delega.
 * Usuários/sessão são tratados aqui; vagas e veículos ficam em SpotService/VehicleService.
 * Todas as requisições passam por um único lock: as validações e gravações (propriedade do veículo,
 * vínculo com vaga, placa única) ficam atômicas mesmo com vários clientes simultâneos.
 */
public class RequestRouter {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private final Object lock = new Object();
    private final UserDao userDao = new UserDao();
    private final SpotDao spotDao = new SpotDao();
    private final SessionManager sessoes = new SessionManager(userDao);
    private final AuthService auth = new AuthService(sessoes, userDao);
    private final VehicleService veiculos = new VehicleService(auth);
    private final SpotService vagas = new SpotService(auth);

    public JsonObject rotearRequisicao(String inputLine, String clientIp) {
        JsonObject req = gson.fromJson(inputLine, JsonObject.class);
        if (req == null) throw new JsonParseException("Requisição vazia");

        JsonElement m = req.get("method");
        if (m == null || !m.isJsonPrimitive()) {
            return Resp.criar(400, "Método não reconhecido", null);
        }
        String method = m.getAsString().toLowerCase();

        JsonElement d = req.get("data");
        JsonObject data = (d != null && d.isJsonObject()) ? d.getAsJsonObject() : new JsonObject();

        synchronized (lock) {
            try {
                return despachar(method, data, clientIp);
            } catch (RespostaException e) {
                return e.paraResposta();
            } catch (SQLException e) {
                ServerLog.log("[Servidor] Erro de banco de dados: " + e.getMessage());
                return Resp.criar(500, Resp.MSG_ERRO_INTERNO, null);
            }
        }
    }

    private JsonObject despachar(String method, JsonObject data, String ip) throws RespostaException, SQLException {
        switch (method) {
            case "login":              return tratarLogin(data, ip);
            case "register":           return tratarRegister(data, ip);
            case "logout":             return tratarLogout(data, ip);
            case "getuser":            return tratarGetUser(data);
            case "updateusername":     return tratarUpdateUserName(data);
            case "updateuserpassword": return tratarUpdateUserSenha(data);
            case "deleteuser":         return tratarDeleteUser(data);

            case "createvaga":         return vagas.criar(data);
            case "getvagas":           return vagas.listar(data);
            case "updatevaga":         return vagas.atualizar(data);
            case "deletevaga":         return vagas.remover(data);

            case "createvehicle":      return veiculos.criar(data);
            case "getvehicles":        return veiculos.listar(data);
            case "getvehicle":         return veiculos.buscar(data);
            case "updatevehicle":      return veiculos.atualizar(data);
            case "deletevehicle":      return veiculos.remover(data);
            case "findvehicle":        return veiculos.localizar(data);

            // extras deste servidor (fora do protocolo da turma)
            case "listlogged":         return tratarListLogged(data);
            case "getuserstatus":      return tratarListaStatus(data);

            default:
                ServerLog.log("[Servidor] Método não reconhecido: " + method);
                return Resp.criar(400, "Método não reconhecido", null);
        }
    }

    // ------------------------ acesso ------------------------

    private JsonObject tratarLogin(JsonObject data, String ip) throws RespostaException, SQLException {
        String username = JsonUtil.texto(data, "username");
        String password = JsonUtil.texto(data, "password");
        if (username == null || password == null) throw Resp.camposInvalidos();

        String senhaArmazenada = userDao.buscarSenhaPorUsername(username);
        if (senhaArmazenada == null || !senhaArmazenada.equals(password)) {
            ServerLog.log("[Servidor] Falha de login para '" + username + "' (" + ip + ")");
            throw new RespostaException(401, "Usuario ou senhas inválidos");
        }

        String token = sessoes.criar(username);
        ServerLog.log("[Servidor] Token gerado para '" + username + "': " + token
                + " (tokens ativos agora: " + sessoes.total() + ")");

        JsonObject dataResp = new JsonObject();
        dataResp.addProperty("token", token);
        return Resp.criar(200, "Login realizado com sucesso", dataResp);
    }

    private JsonObject tratarLogout(JsonObject data, String ip) throws RespostaException {
        String token = JsonUtil.texto(data, "token");
        if (token == null || token.trim().isEmpty()) {
            throw new RespostaException(400, Resp.MSG_TOKEN_AUSENTE);
        }
        String username = sessoes.encerrar(token);
        if (username == null) throw new RespostaException(401, Resp.MSG_SESSAO_EXPIRADA);

        ServerLog.log("[Servidor] Logout de '" + username + "' (tokens ativos agora: " + sessoes.total() + ")");
        return Resp.criar(200, "Logout realizado com sucesso", null);
    }

    // ------------------------ usuários ------------------------

    /**
     * register é o mesmo endpoint para user e admin:
     *  - sem role e sem token  -> cria "user";
     *  - com role e token      -> o token precisa ser de um admin; cria com a role informada;
     *  - combinação incompleta/inválida -> erro e nada é criado.
     */
    private JsonObject tratarRegister(JsonObject data, String ip) throws RespostaException, SQLException {
        boolean temRole = data.has("role") && !data.get("role").isJsonNull();
        boolean temToken = data.has("token") && !data.get("token").isJsonNull();

        String role = User.ROLE_USER;
        if (temRole != temToken) {
            throw new RespostaException(400, "Combinação incompleta: role e token devem ser informados juntos.");
        }
        if (temRole) {
            String roleInformada = JsonUtil.texto(data, "role");
            if (!Validator.role(roleInformada)) throw new RespostaException(400, campoInvalido("role"));

            AuthContext ctx = auth.autenticar(data); // 400 (sem token) ou 401 "Sessão expirada ou encerrada."
            if (!ctx.isAdmin()) {
                String msg = User.ROLE_ADMIN.equals(roleInformada)
                        ? "Operação não autorizada. Apenas administradores podem cadastrar administradores."
                        : "Operação não autorizada. Apenas administradores podem informar o perfil do usuário.";
                throw new RespostaException(401, msg);
            }
            role = roleInformada;
        }

        String name = valor(JsonUtil.texto(data, "name")).trim();
        String username = valor(JsonUtil.texto(data, "username")).trim();
        String password = valor(JsonUtil.texto(data, "password"));

        if (userDao.usernameExiste(username)) {
            throw new RespostaException(409, "O username já está em uso.");
        }
        if (!Validator.name(name)) throw new RespostaException(400, campoInvalido("name"));
        if (!Validator.username(username)) throw new RespostaException(400, campoInvalido("username"));
        if (!Validator.password(password)) throw new RespostaException(400, campoInvalido("password"));

        try {
            userDao.cadastrarUsuario(new User(name, username, password, role));
        } catch (SQLException e) {
            if (SqlUtil.ehViolacaoDeIntegridade(e)) throw new RespostaException(409, "O username já está em uso.");
            throw e;
        }
        ServerLog.log("[Servidor] Novo usuário cadastrado: '" + username + "' role=" + role + " (" + ip + ")");
        return Resp.criar(201, "Usuário criado com sucesso", null);
    }

    /** getuser: admin consulta qualquer usuário (campo username); user recebe os próprios dados. */
    private JsonObject tratarGetUser(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);

        String alvo = ctx.getUsername();
        String pedido = JsonUtil.texto(data, "username");
        if (ctx.isAdmin() && pedido != null && !pedido.trim().isEmpty()) alvo = pedido;

        User user = userDao.buscarPorUsername(alvo);
        if (user == null) throw new RespostaException(404, "Usuário não encontrado");

        JsonObject d = new JsonObject();
        d.addProperty("name", user.getName());
        d.addProperty("username", user.getUsername());
        d.addProperty("role", user.getRole());
        return Resp.criar(200, "Usuário encontrado com sucesso", d);
    }

    /** updateusername: admin altera o name de qualquer usuário; user só o próprio. */
    private JsonObject tratarUpdateUserName(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);

        String alvo = JsonUtil.texto(data, "username");
        if (alvo == null || alvo.trim().isEmpty()) alvo = ctx.getUsername();
        if (!ctx.isAdmin() && !alvo.equals(ctx.getUsername())) {
            throw new RespostaException(401, "Operação não autorizada");
        }

        String novoNome = valor(JsonUtil.texto(data, "name")).trim();
        if (!Validator.name(novoNome)) throw new RespostaException(400, "Nome do usuário fora do padrão");

        if (userDao.buscarPorUsername(alvo) == null) throw new RespostaException(404, "Usuário não encontrado");
        if (!userDao.atualizarNome(alvo, novoNome)) throw new RespostaException(404, "Usuário não encontrado");

        ServerLog.log("[Servidor] Nome de '" + alvo + "' atualizado por '" + ctx.getUsername() + "'");
        return Resp.criar(200, "Nome do usuário atualizado com sucesso", null);
    }

    private JsonObject tratarUpdateUserSenha(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);

        String informado = JsonUtil.texto(data, "username");
        if (informado != null && !informado.isEmpty() && !informado.equals(ctx.getUsername())) {
            throw new RespostaException(401, "Operação não autorizada");
        }
        String oldPassword = valor(JsonUtil.texto(data, "oldPassword"));
        String newPassword = valor(JsonUtil.texto(data, "newPassword"));

        String senhaAtual = userDao.buscarSenhaPorUsername(ctx.getUsername());
        if (senhaAtual == null) throw new RespostaException(404, "Usuário não encontrado");
        if (!senhaAtual.equals(oldPassword)) {
            ServerLog.log("[Servidor] Senha atual incorreta para '" + ctx.getUsername() + "'");
            throw new RespostaException(401, "Senha atual incorreta");
        }
        if (!Validator.password(newPassword)) throw new RespostaException(400, "Senha fora do padrão");
        if (newPassword.equals(oldPassword)) {
            throw new RespostaException(400, "A nova senha não pode ser igual a anterior");
        }

        userDao.atualizarSenha(ctx.getUsername(), newPassword);
        return Resp.criar(200, "Senha atualizada com sucesso", null);
    }

    /**
     * deleteuser: user exclui a própria conta; admin exclui a de qualquer outro usuário.
     * Bloqueado (409) se o dono tiver veículo estacionado. Veículos e sessões são removidos em cascata.
     */
    private JsonObject tratarDeleteUser(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);

        String alvo = JsonUtil.texto(data, "username");
        if (alvo == null || alvo.trim().isEmpty()) throw Resp.camposInvalidos();

        if (!ctx.isAdmin() && !alvo.equals(ctx.getUsername())) {
            throw new RespostaException(401, "Operação não autorizada");
        }
        if (ctx.isAdmin() && alvo.equals(ctx.getUsername())) {
            throw new RespostaException(401, "Operação não autorizada. O administrador não pode excluir a própria conta.");
        }
        if (userDao.buscarPorUsername(alvo) == null) throw new RespostaException(404, "Usuário não encontrado");

        if (spotDao.usuarioTemVeiculoEstacionado(alvo)) {
            throw new RespostaException(409, "Usuário possui veículo estacionado. Libere a vaga antes desta operação.");
        }
        if (!userDao.deletarUsuarioCompleto(alvo)) throw new RespostaException(404, "Usuário não encontrado");

        sessoes.encerrarDoUsuario(alvo); // invalida as sessões da conta excluída
        ServerLog.log("[Servidor] Conta '" + alvo + "' excluída por '" + ctx.getUsername() + "'");
        return Resp.criar(200, "Usuário deletado com sucesso", null);
    }

    // ------------------------ extras (fora do protocolo) ------------------------

    private JsonObject tratarListLogged(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem executar esta ação.");

        JsonArray lista = new JsonArray();
        for (String u : sessoes.usuariosLogados()) lista.add(new JsonPrimitive(u));
        JsonObject d = new JsonObject();
        d.add("users", lista);
        return Resp.criar(200, "Usuários logados listados com sucesso", d);
    }

    private JsonObject tratarListaStatus(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem executar esta ação.");

        JsonObject d = new JsonObject();
        d.add("users", userDao.listarStatusUsuarios());
        return Resp.criar(200, "Lista de status dos usuários obtida com sucesso", d);
    }

    // ------------------------ auxiliares ------------------------

    private String valor(String s) {
        return s == null ? "" : s;
    }

    private String campoInvalido(String campo) {
        return "O campo " + campo + " não está no padrão esperado.";
    }

    /** Mantido para o ClientHandler. */
    public JsonObject criarResposta(int statusCode, String message, JsonObject data) {
        return Resp.criar(statusCode, message, data);
    }
}
