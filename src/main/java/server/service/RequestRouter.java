package server.service;

import com.google.gson.*;
import server.database.UserDao;
import server.model.User;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RequestRouter {

    private static final Map<String, String> sessoesAtivas = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private final UserDao userDao = new UserDao();

    public JsonObject rotearRequisicao(String inputLine, String clientIp) {
        JsonObject req = gson.fromJson(inputLine, JsonObject.class);
        if (req == null) throw new JsonParseException("Requisição vazia");

        String method = req.has("method") ? req.get("method").getAsString().toLowerCase() : "";
        JsonObject data = req.has("data") && !req.get("data").isJsonNull() ? req.getAsJsonObject("data") : new JsonObject();

        System.out.println("\n[REQUISIÇÃO RECEBIDA] IP: " + clientIp + " | Método: " + method + " " + req);

        switch (method) {
            case "login":
                return tratarLogin(data, clientIp);
            case "register":
                return tratarRegister(data, clientIp);
            case "logout":
                return tratarLogout(data, clientIp);
            case "getuser":
                return tratarGetUser(data, clientIp);
            case "updateuserpassword":
                return tratarUpdateUserSenha(data, clientIp);
            case "updateusername":
                return tratarUpdateUserName(data, clientIp);
            case "deleteuser":
                return tratarDeleteUser(data, clientIp);
            case "listlogged":
                return tratarListLogged(data, clientIp);
            case "getuserstatus":
                return tratarListaStatus(data, clientIp);
            default:
                System.out.println("[ALERTA] Método não reconhecido: " + method);
                return criarResposta(400, "Método não reconhecido", null);
        }
    }

    private JsonObject tratarLogin(JsonObject data, String clientIp) {
        String username = getString(data, "username");
        String password = getString(data, "password");

        try {
            String senhaArmazenada = userDao.buscarSenhaPorUsername(username);

            if (senhaArmazenada == null) {
                System.out.println("[FALHA LOGIN] Usuário '" + username + "' não encontrado (" + clientIp + ")");
                return criarResposta(404, "Usuário não encontrado", null);
            }

            if (password.equals(senhaArmazenada)) {
                String token = UUID.randomUUID().toString();
                sessoesAtivas.put(token, username);
                userDao.registrarSessao(username, token);

                System.out.println("[SUCESSO LOGIN] Usuário '" + username + "' logou com sucesso de " + clientIp);

                JsonObject dataResp = new JsonObject();
                dataResp.addProperty("token", token);
                return criarResposta(200, "Sucesso no Login", dataResp);
            } else {
                System.out.println("[FALHA LOGIN] Senha incorreta para o usuário '" + username + "' (" + clientIp + ")");
                return criarResposta(401, "Senha incorreta", null);
            }

        } catch (SQLException e) {
            System.err.println("[ERRO BD LOGIN] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private JsonObject tratarRegister(JsonObject data, String clientIp) {
        String name = getString(data, "name").trim();
        String username = getString(data, "username").trim();
        String password = getString(data, "password");

        try {
            if (userDao.usernameExiste(username)) {
                System.out.println("[FALHA CADASTRO] Usuário '" + username + "' já existe (" + clientIp + ")");
                return criarResposta(409, "Usuário já cadastrado", null);
            }

            if (!validarName(name)) {
                System.out.println("[FALHA CADASTRO] Campo name inválido (" + clientIp + ")");
                return criarResposta(400, "Campo name fora do padrão exigido", null);
            }
            if (!validarUsername(username)) {
                System.out.println("[FALHA CADASTRO] Campo username inválido (" + clientIp + ")");
                return criarResposta(400, "Campo username fora do padrão exigido", null);
            }
            if (!validarPassword(password)) {
                System.out.println("[FALHA CADASTRO] Campo password inválido (" + clientIp + ")");
                return criarResposta(400, "Campo password fora do padrão exigido", null);
            }

            userDao.cadastrarUsuario(new User(name, username, password));

            System.out.println("[SUCESSO CADASTRO] Novo usuário cadastrado: '" + username + "' (" + clientIp + ")");
            return criarResposta(200, "Usuário criado com sucesso", null);

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                System.out.println("[FALHA CADASTRO] Usuário '" + username + "' já existe (" + clientIp + ")");
                return criarResposta(409, "Usuário já cadastrado", null);
            }
            System.err.println("[ERRO BD CADASTRO] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private JsonObject tratarLogout(JsonObject data, String clientIp) {
        String token = getString(data, "token");

        String username = sessoesAtivas.remove(token);

        if (username != null) {
            userDao.encerrarSessao(token);
            System.out.println("[LOGOUT] Usuário '" + username + "' efetuou logout (" + clientIp + ")");
            return criarResposta(200, "Usuário deslogado com sucesso", null);
        } else {
            System.out.println("[FALHA LOGOUT] Token inválido enviado de " + clientIp);
            return criarResposta(401, "Token de sessão inválido ou expirado", null);
        }
    }

    private JsonObject tratarGetUser(JsonObject data, String clientIp) {
        String token = getString(data, "token");
        String username = sessoesAtivas.get(token);

        if (username == null) {
            System.out.println("[FALHA BUSCA] Token não autorizado (" + clientIp + ")");
            return criarResposta(401, "Acesso não autorizado ou token inválido", null);
        }

        try {
            User user = userDao.buscarPorUsername(username);

            if (user != null) {
                JsonObject userData = new JsonObject();
                userData.addProperty("name", user.getName());
                userData.addProperty("username", user.getUsername());
                userData.addProperty("creationDate", user.getCreationDate()); // extra: controle futuro

                System.out.println("[SUCESSO BUSCA] Dados consultados pelo usuário '" + username + "' (" + clientIp + ")");
                return criarResposta(200, "Usuário encontrado com sucesso", userData);
            } else {
                System.out.println("[FALHA BUSCA] Dados não encontrados para '" + username + "'");
                return criarResposta(404, "Dados de usuário não encontrados", null);
            }

        } catch (SQLException e) {
            System.err.println("[ERRO BD BUSCA] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private JsonObject tratarUpdateUserSenha(JsonObject data, String clientIp) {
        String token = getString(data, "token");
        String oldPassword = getString(data, "oldPassword");
        String newPassword = getString(data, "newPassword");

        String username = sessoesAtivas.get(token);
        if (username == null) {
            System.out.println("[FALHA UPDATE] Token não autorizado (" + clientIp + ")");
            return criarResposta(401, "Sessão inválida ou expirada", null);
        }

        if (!validarPassword(newPassword)) {
            System.out.println("[FALHA UPDATE] Nova senha fora dos padrões para '" + username + "'");
            return criarResposta(400, "Campo newPassword fora do padrão exigido", null);
        }

        try {
            String senhaAtual = userDao.buscarSenhaPorUsername(username);

            if (senhaAtual == null) {
                return criarResposta(404, "Usuário não encontrado", null);
            }

            if (!senhaAtual.equals(oldPassword)) {
                return criarResposta(401, "Senha atual incorreta", null);
            }

            userDao.atualizarSenha(username, newPassword);
            System.out.println("[SUCESSO UPDATE] Senha atualizada pelo usuário '" + username + "' (" + clientIp + ")");
            return criarResposta(200, "Senha atualizada com sucesso", null);

        } catch (SQLException e) {
            System.err.println("[ERRO BD UPDATE] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private JsonObject tratarUpdateUserName(JsonObject data, String clientIp) {
        String token = getString(data, "token");
        String newName = getString(data, "name").trim();

        String username = sessoesAtivas.get(token);
        if (username == null) {
            System.out.println("[FALHA UPDATE] Token não autorizado (" + clientIp + ")");
            return criarResposta(401, "Sessão inválida ou expirada", null);
        }

        if (!validarName(newName)) {
            System.out.println("[FALHA UPDATE] Novo nome fora dos padrões para '" + username + "'");
            return criarResposta(400, "Campo name fora do padrão exigido", null);
        }

        try {
            if (userDao.atualizarNome(username, newName)) {
                System.out.println("[SUCESSO UPDATE] Nome atualizado pelo usuário '" + username + "' (" + clientIp + ")");
                return criarResposta(200, "Usuário atualizada com sucesso", null);
            } else {
                return criarResposta(404, "Usuário não encontrado para atualização", null);
            }

        } catch (SQLException e) {
            System.err.println("[ERRO BD UPDATE] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private JsonObject tratarDeleteUser(JsonObject data, String clientIp) {
        String token = getString(data, "token");
        String username = sessoesAtivas.get(token);

        if (username == null) {
            System.out.println("[FALHA DELETE] Token não autorizado (" + clientIp + ")");
            return criarResposta(401, "Sessão inválida ou expirada", null);
        }

        try {
            if (userDao.ehAdmin(username)) {
                System.out.println("[FALHA DELETE] Tentativa de excluir conta admin (" + clientIp + ")");
                return criarResposta(403, "A conta de administrador não pode ser excluída", null);
            }

            userDao.encerrarSessoesDoUsuario(username);
            if (userDao.deletarUsuario(username)) {
                sessoesAtivas.values().removeIf(u -> u.equals(username));
                System.out.println("[SUCESSO DELETE] Conta do usuário '" + username + "' foi excluída (" + clientIp + ")");
                return criarResposta(200, "Usuário deletado com sucesso", null);
            } else {
                return criarResposta(404, "Usuário não encontrado para exclusão", null);
            }

        } catch (SQLException e) {
            System.err.println("[ERRO BD DELETE] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    // ---- Funções de administrador ----

    private JsonObject tratarListLogged(JsonObject data, String clientIp) {
        String username = sessoesAtivas.get(getString(data, "token"));

        if (username == null) {
            System.out.println("[FALHA ADMIN] Token não autorizado (" + clientIp + ")");
            return criarResposta(401, "Sessão inválida ou expirada", null);
        }

        try {
            if (!userDao.ehAdmin(username)) {
                System.out.println("[FALHA ADMIN] '" + username + "' tentou acessar função de admin (" + clientIp + ")");
                return criarResposta(403, "Acesso restrito ao administrador", null);
            }

            JsonArray lista = new JsonArray();
            new TreeSet<>(sessoesAtivas.values()).forEach(lista::add); // usernames únicos, em ordem

            JsonObject dataResp = new JsonObject();
            dataResp.add("users", lista);

            System.out.println("[SUCESSO ADMIN] '" + username + "' listou usuários logados (" + clientIp + ")");
            return criarResposta(200, "Usuários logados listados com sucesso", dataResp);

        } catch (SQLException e) {
            System.err.println("[ERRO BD ADMIN] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private JsonObject tratarListaStatus(JsonObject data, String clientIp){
        String username = sessoesAtivas.get(getString(data, "token"));

        if (username == null) {
            System.out.println("[FALHA ADMIN] Token não autorizado (" + clientIp + ")");
            return criarResposta(401, "Sessão inválida ou expirada", null);
        }

        try {
            if (!userDao.ehAdmin(username)) {
                System.out.println("[FALHA ADMIN] '" + username + "' tentou acessar função de admin (" + clientIp + ")");
                return criarResposta(403, "Acesso restrito ao administrador", null);
            }

            JsonArray listaStatus = userDao.listarStatusUsuarios();

            JsonObject dataResp = new JsonObject();
            dataResp.add("users", listaStatus);

            System.out.println("[SUCESSO ADMIN] '" + username + "' consultou lista de status de usuários (" + clientIp + ")");
            return criarResposta(200, "Lista de status dos usuários obtida com sucesso", dataResp);

        } catch (SQLException e) {
            System.err.println("[ERRO BD ADMIN] " + e.getMessage());
            return criarResposta(500, "Erro interno no banco de dados", null);
        }
    }

    private String getString(JsonObject data, String key) {
        JsonElement el = data.get(key);
        if (el == null || !el.isJsonPrimitive()) return "";
        return el.getAsString();
    }

    private boolean validarName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return Pattern.matches("^[\\p{L} ]{1,60}$", name);
    }

    private boolean validarUsername(String username) {
        if (username == null) return false;
        return Pattern.matches("^[a-z0-9._]{3,20}$", username);
    }

    private boolean validarPassword(String password) {
        if (password == null) return false;
        return Pattern.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[#.*&%$@!()_=+-])[a-zA-Z0-9#.*&%$@!()_=+-]{8,20}$",
                password);
    }

    public JsonObject criarResposta(int statusCode, String message, JsonObject data) {
        JsonObject resp = new JsonObject();
        resp.addProperty("statusCode", statusCode);
        resp.addProperty("message", message);
        resp.add("data", data == null ? JsonNull.INSTANCE : data);
        return resp;
    }
}