package server.database;

import java.sql.SQLException;

public final class SqlUtil {
    private SqlUtil() {}

    /** Violação de chave única/integridade (MySQL: SQLState 23000, erro 1062). */
    public static boolean ehViolacaoDeIntegridade(SQLException e) {
        String estado = e.getSQLState();
        return estado != null && estado.startsWith("23");
    }
}
