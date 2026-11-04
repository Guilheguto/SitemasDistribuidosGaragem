package client.model;

public class UserSession {
    private String token;
    private String username;
    private String name;
    private String role;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }

    public void clear() {
        this.token = null;
        this.username = "";
        this.name = "";
        this.role = "user";
    }
}
