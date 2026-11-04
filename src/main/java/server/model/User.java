package server.model;

public class User {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    private final String name;
    private final String username;
    private final String password;
    private final String role;

    public User(String name, String username, String password, String role) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
}
