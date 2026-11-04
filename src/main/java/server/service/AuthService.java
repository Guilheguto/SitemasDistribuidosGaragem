package server.service;

import com.google.gson.JsonObject;
import server.database.UserDao;
import server.model.User;

import java.sql.SQLException;

public class AuthService {

    private final SessionManager sessoes;
    private final UserDao userDao;

    public AuthService(SessionManager sessoes, UserDao userDao) {
        this.sessoes = sessoes;
        this.userDao = userDao;
    }

    /**
     * Valida o token da requisição.
     * - sem token: 400; token inválido/encerrado: 401 "Sessão expirada ou encerrada."
     * O perfil é lido do banco a cada requisição (não do cliente).
     */
    public AuthContext autenticar(JsonObject data) throws RespostaException, SQLException {
        String token = JsonUtil.texto(data, "token");
        if (token == null || token.trim().isEmpty()) {
            throw new RespostaException(400, Resp.MSG_TOKEN_AUSENTE);
        }
        String username = sessoes.usernameDoToken(token);
        if (username == null) {
            throw new RespostaException(401, Resp.MSG_SESSAO_EXPIRADA);
        }
        User user = userDao.buscarPorUsername(username);
        if (user == null) {
            throw new RespostaException(401, Resp.MSG_SESSAO_EXPIRADA);
        }
        return new AuthContext(username, user.isAdmin());
    }

    /** Token válido, mas sem permissão: 401 com mensagem específica. */
    public void exigirAdmin(AuthContext ctx, String mensagem) throws RespostaException {
        if (!ctx.isAdmin()) throw new RespostaException(401, mensagem);
    }

    public void exigirUsuarioComum(AuthContext ctx) throws RespostaException {
        if (ctx.isAdmin()) {
            throw new RespostaException(401, "Operação não autorizada. Apenas usuários comuns podem executar esta ação.");
        }
    }
}
