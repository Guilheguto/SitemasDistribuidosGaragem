package server.model;

public class Vehicle {
    private final String licensePlate;
    private final String brand;
    private final String model;
    private final String type;
    private final String ownerUsername;

    public Vehicle(String licensePlate, String brand, String model, String type, String ownerUsername) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.ownerUsername = ownerUsername;
    }

    public String getLicensePlate() { return licensePlate; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getType() { return type; }
    public String getOwnerUsername() { return ownerUsername; }
}
