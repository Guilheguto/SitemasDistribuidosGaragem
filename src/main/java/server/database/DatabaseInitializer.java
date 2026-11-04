package server.database;

import server.model.User;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cria as tabelas que faltarem, garante as colunas necessárias e a conta admin inicial.
 * Pode ser executado várias vezes sem efeito colateral.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    public static void inicializar() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "name VARCHAR(60) NOT NULL, " +
                    "username VARCHAR(20) NOT NULL PRIMARY KEY, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "is_admin BOOLEAN NOT NULL DEFAULT FALSE)");
            garantirColuna(conn, "users", "is_admin", "BOOLEAN NOT NULL DEFAULT FALSE");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS active_sessions (" +
                    "token VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "username VARCHAR(20) NOT NULL, " +
                    "INDEX idx_sessions_username (username))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS vehicles (" +
                    "license_plate VARCHAR(7) NOT NULL PRIMARY KEY, " +
                    "owner_username VARCHAR(20) NOT NULL, " +
                    "brand VARCHAR(20) NOT NULL, " +
                    "model VARCHAR(20) NULL, " +
                    "vehicle_type VARCHAR(10) NOT NULL, " +
                    "INDEX idx_vehicles_owner (owner_username))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS parking_spots (" +
                    "spot_name VARCHAR(4) NOT NULL PRIMARY KEY, " +
                    "floor_number INT NOT NULL, " +
                    "spot_type VARCHAR(10) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'disponivel', " +
                    "license_plate VARCHAR(7) NULL UNIQUE)");
        }

        UserDao userDao = new UserDao();
        userDao.limparSessoes(); // tokens ficam em memória: não sobrevivem a um reinício
        if (!userDao.usernameExiste("admin")) {
            userDao.cadastrarUsuario(new User("admin", "admin", "Admin123!", User.ROLE_ADMIN));
        } else {
            userDao.definirAdmin("admin");
        }
    }

    private static void garantirColuna(Connection conn, String tabela, String coluna, String definicao) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabela, coluna)) {
            if (rs.next()) return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + definicao);
        }
    }
}
