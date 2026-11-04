package server.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import server.model.User;

import java.sql.*;

public class UserDao {

    public boolean usernameExiste(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean cadastrarUsuario(User user) throws SQLException {
        String sql = "INSERT INTO users (name, username, password, is_admin) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setBoolean(4, user.isAdmin());
            return stmt.executeUpdate() > 0;
        }
    }

    public String buscarSenhaPorUsername(String username) throws SQLException {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("password");
            }
        }
        return null;
    }

    public User buscarPorUsername(String username) throws SQLException {
        String sql = "SELECT name, username, is_admin FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getBoolean("is_admin") ? User.ROLE_ADMIN : User.ROLE_USER;
                    return new User(rs.getString("name"), rs.getString("username"), null, role);
                }
            }
        }
        return null;
    }

    public boolean atualizarSenha(String username, String novaSenha) throws SQLException {
        return executar("UPDATE users SET password = ? WHERE username = ?", novaSenha, username) > 0;
    }

    public boolean atualizarNome(String username, String novoNome) throws SQLException {
        return executar("UPDATE users SET name = ? WHERE username = ?", novoNome, username) > 0;
    }

    public void definirAdmin(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET is_admin = TRUE WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    public boolean deletarUsuarioCompleto(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                executar(conn, "DELETE FROM active_sessions WHERE username = ?", username);
                executar(conn, "DELETE FROM vehicles WHERE owner_username = ?", username);
                int linhas = executar(conn, "DELETE FROM users WHERE username = ?", username);
                conn.commit();
                return linhas > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ------------------------ CONTROLE DE SESSÃO ------------------------

    public void registrarSessao(String username, String token) throws SQLException {
        executar("INSERT INTO active_sessions (username, token) VALUES (?, ?)", username, token);
    }

    public void encerrarSessao(String token) {
        try {
            executar("DELETE FROM active_sessions WHERE token = ?", token);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void encerrarSessoesDoUsuario(String username) {
        try {
            executar("DELETE FROM active_sessions WHERE username = ?", username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void limparSessoes() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM active_sessions");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public JsonArray listarStatusUsuarios() throws SQLException {
        JsonArray lista = new JsonArray();
        String sql = "SELECT u.username, " +
                "CASE WHEN EXISTS (SELECT 1 FROM active_sessions s WHERE s.username = u.username) " +
                "THEN 'ONLINE' ELSE 'OFFLINE' END AS status " +
                "FROM users u ORDER BY u.username";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                JsonObject item = new JsonObject();
                item.addProperty("username", rs.getString("username"));
                item.addProperty("status", rs.getString("status"));
                lista.add(item);
            }
        }
        return lista;
    }

    // ------------------------ utilitários ------------------------

    private int executar(String sql, String... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return executar(conn, sql, params);
        }
    }

    private int executar(Connection conn, String sql, String... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) stmt.setString(i + 1, params[i]);
            return stmt.executeUpdate();
        }
    }
}
