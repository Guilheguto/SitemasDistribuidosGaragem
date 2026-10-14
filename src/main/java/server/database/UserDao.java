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
            return stmt.executeQuery().next();
        }
    }

    public boolean cadastrarUsuario(User user) throws SQLException {
        String sql = "INSERT INTO users (name, username, password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            return stmt.executeUpdate() > 0;
        }
    }

    public String buscarSenhaPorUsername(String username) throws SQLException {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
        }
        return null;
    }

    public User buscarPorUsername(String username) throws SQLException {
        String sql = "SELECT name, username, creation_date FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getString("name"),
                        rs.getString("username"),
                        null,
                        rs.getString("creation_date")
                );
            }
        }
        return null;
    }

    public boolean atualizarSenha(String username, String novaSenha) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novaSenha);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean atualizarNome(String username, String novoNome) throws SQLException {
        String sql = "UPDATE users SET name = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean ehAdmin(String username) throws SQLException {
        String sql = "SELECT is_admin FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("is_admin");
        }
    }

    public boolean deletarUsuario(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        }
    }

    //------------------------CONTROLE DE SESSÃO--------------------------
    public String registrarSessao(String username, String token) {
        String sql = "INSERT INTO active_sessions (username, token) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, token);
            stmt.executeUpdate();

            return token;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean encerrarSessao(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String sql = "DELETE FROM active_sessions WHERE token = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            int linhasAfetadas = stmt.executeUpdate();

            return linhasAfetadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public void limparSessoes() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM active_sessions")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void encerrarSessoesDoUsuario(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM active_sessions WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
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
}