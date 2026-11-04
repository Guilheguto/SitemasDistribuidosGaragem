package server.service;

/** Quem está fazendo a requisição (obtido do token + perfil persistido no banco). */
public class AuthContext {
    private final String username;
    private final boolean admin;

    public AuthContext(String username, boolean admin) {
        this.username = username;
        this.admin = admin;
    }

    public String getUsername() { return username; }
    public boolean isAdmin() { return admin; }
}
