package client.model;

public class UserSession {
    private String token;
    private String username;
    private String name;

    public UserSession() {}

    public String getToken() {
        return token;
    }
    public void setToken(String token) {

        this.token = token;
    }

    public String getUsername() {

        return username;
    }
    public void setUsername(String username) {

        this.username = username;
    }

    public String getName() {

        return name;
    }
    public void setName(String name) {

        this.name = name;
    }

    public void clear() {
        this.token = null;
        this.username = "";
        this.name = "";
    }
}