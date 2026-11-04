package server.database;

import server.model.ParkingSpot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SpotDao {

    private static final String COLUNAS = "spot_name, floor_number, spot_type, status, license_plate";

    public void inserir(String nome, int andar, String tipo) throws SQLException {
        String sql = "INSERT INTO parking_spots (spot_name, floor_number, spot_type, status, license_plate) " +
                "VALUES (?, ?, ?, 'disponivel', NULL)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setInt(2, andar);
            stmt.setString(3, tipo);
            stmt.executeUpdate();
        }
    }

    public boolean existeNome(String nome) throws SQLException {
        return buscarPorNome(nome) != null;
    }

    public ParkingSpot buscarPorNome(String nome) throws SQLException {
        return buscarUma("SELECT " + COLUNAS + " FROM parking_spots WHERE spot_name = ?", nome);
    }

    public ParkingSpot buscarPorPlaca(String placa) throws SQLException {
        return buscarUma("SELECT " + COLUNAS + " FROM parking_spots WHERE license_plate = ?", placa);
    }

    public List<ParkingSpot> listar() throws SQLException {
        List<ParkingSpot> lista = new ArrayList<>();
        String sql = "SELECT " + COLUNAS + " FROM parking_spots ORDER BY floor_number, spot_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Grava o estado final da vaga em uma única instrução (atômico). */
    public boolean atualizar(String nomeAtual, String novoNome, int andar, String status, String placa) throws SQLException {
        String sql = "UPDATE parking_spots SET spot_name = ?, floor_number = ?, status = ?, license_plate = ? WHERE spot_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setInt(2, andar);
            stmt.setString(3, status);
            stmt.setString(4, placa);
            stmt.setString(5, nomeAtual);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean remover(String nome) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM parking_spots WHERE spot_name = ?")) {
            stmt.setString(1, nome);
            return stmt.executeUpdate() > 0;
        }
    }

    /** O usuário possui algum veículo atualmente em uma vaga? */
    public boolean usuarioTemVeiculoEstacionado(String owner) throws SQLException {
        String sql = "SELECT 1 FROM parking_spots s JOIN vehicles v ON s.license_plate = v.license_plate " +
                "WHERE v.owner_username = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, owner);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private ParkingSpot buscarUma(String sql, String parametro) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parametro);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private ParkingSpot mapear(ResultSet rs) throws SQLException {
        return new ParkingSpot(rs.getString("spot_name"), rs.getInt("floor_number"),
                rs.getString("spot_type"), rs.getString("status"), rs.getString("license_plate"));
    }
}
