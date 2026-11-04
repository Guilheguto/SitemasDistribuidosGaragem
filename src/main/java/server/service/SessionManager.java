package server.service;

import server.database.UserDao;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tokens ativos (memória) sincronizados com a tabela active_sessions. */
public class SessionManager {

    private final Map<String, String> sessoes = new ConcurrentHashMap<>();
    private final UserDao userDao;

    public SessionManager(UserDao userDao) {
        this.userDao = userDao;
    }

    public String criar(String username) throws SQLException {
        String token = UUID.randomUUID().toString();
        userDao.registrarSessao(username, token);
        sessoes.put(token, username);
        return token;
    }

    /** Username dono do token, ou null se inválido/encerrado. */
    public String usernameDoToken(String token) {
        return token == null ? null : sessoes.get(token);
    }

    /** Encerra o token; devolve o username ou null se o token não existia. */
    public String encerrar(String token) {
        String username = sessoes.remove(token);
        if (username != null) userDao.encerrarSessao(token);
        return username;
    }

    public void encerrarDoUsuario(String username) {
        sessoes.values().removeIf(u -> u.equals(username));
        userDao.encerrarSessoesDoUsuario(username);
    }

    public int total() {
        return sessoes.size();
    }

    public TreeSet<String> usuariosLogados() {
        return new TreeSet<>(sessoes.values());
    }
}
