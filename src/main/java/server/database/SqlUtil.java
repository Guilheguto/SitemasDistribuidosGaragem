package server.database;

import java.sql.SQLException;

public final class SqlUtil {
    private SqlUtil() {}

    public static boolean ehViolacaoDeIntegridade(SQLException e) {
        String estado = e.getSQLState();
        return estado != null && estado.startsWith("23");
    }
}
