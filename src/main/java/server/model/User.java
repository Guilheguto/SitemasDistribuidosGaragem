package server.model;

public class User {
    private String name;
    private String username;
    private String password;
    private String creationDate;

    public User(String name, String username, String password, String creationDate) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.creationDate = creationDate;
    }

    public User(String name, String username, String password) {
        this(name, username, password, null);
    }

    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getCreationDate() { return creationDate; }
}