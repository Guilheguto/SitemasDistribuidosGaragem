package server.database;

import server.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    private static final String COLUNAS = "license_plate, brand, model, vehicle_type, owner_username";

    public void inserir(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicles (license_plate, brand, model, vehicle_type, owner_username) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, v.getLicensePlate());
            stmt.setString(2, v.getBrand());
            stmt.setString(3, v.getModel());
            stmt.setString(4, v.getType());
            stmt.setString(5, v.getOwnerUsername());
            stmt.executeUpdate();
        }
    }

    public Vehicle buscarPorPlaca(String placa) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM vehicles WHERE license_plate = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, placa);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Vehicle> listarPorProprietario(String owner) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM vehicles WHERE owner_username = ? ORDER BY license_plate";
        List<Vehicle> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, owner);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Grava o estado final completo (a placa e o proprietário nunca mudam). */
    public boolean atualizar(String placa, String brand, String model, String type) throws SQLException {
        String sql = "UPDATE vehicles SET brand = ?, model = ?, vehicle_type = ? WHERE license_plate = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, brand);
            stmt.setString(2, model);
            stmt.setString(3, type);
            stmt.setString(4, placa);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean remover(String placa) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM vehicles WHERE license_plate = ?")) {
            stmt.setString(1, placa);
            return stmt.executeUpdate() > 0;
        }
    }

    private Vehicle mapear(ResultSet rs) throws SQLException {
        return new Vehicle(rs.getString("license_plate"), rs.getString("brand"), rs.getString("model"),
                rs.getString("vehicle_type"), rs.getString("owner_username"));
    }
}
