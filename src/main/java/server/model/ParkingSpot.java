package server.model;

public class ParkingSpot {
    public static final String DISPONIVEL = "disponivel";
    public static final String OCUPADA = "ocupada";
    public static final String FORA_DE_SERVICO = "fora_de_servico";

    private final String spotName;
    private final int floor;
    private final String type;
    private final String status;
    private final String licensePlate; // null quando não há veículo

    public ParkingSpot(String spotName, int floor, String type, String status, String licensePlate) {
        this.spotName = spotName;
        this.floor = floor;
        this.type = type;
        this.status = status;
        this.licensePlate = licensePlate;
    }

    public String getSpotName() { return spotName; }
    public int getFloor() { return floor; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getLicensePlate() { return licensePlate; }
}
