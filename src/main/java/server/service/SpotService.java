package server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import server.database.SpotDao;
import server.database.SqlUtil;
import server.database.VehicleDao;
import server.model.ParkingSpot;
import server.model.Vehicle;

import java.sql.SQLException;

public class SpotService {

    private static final String MSG_VAGA_NAO_ENCONTRADA = "Vaga não encontrada.";
    private static final String MSG_VAGA_OCUPADA = "Vaga ocupada. Libere a vaga antes desta operação.";
    private static final String MSG_NOME_DUPLICADO = "Já existe uma vaga com este nome.";

    private final AuthService auth;
    private final SpotDao spotDao = new SpotDao();
    private final VehicleDao vehicleDao = new VehicleDao();

    public SpotService(AuthService auth) {
        this.auth = auth;
    }

    /** createvaga (admin). O status inicial "disponivel" é atribuído pelo servidor. */
    public JsonObject criar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem cadastrar vagas.");

        String nome = JsonUtil.texto(data, "spotName");
        Integer andar = JsonUtil.inteiro(data, "floor");
        String tipo = JsonUtil.texto(data, "type");
        if (!Validator.spotName(nome) || andar == null || !Validator.type(tipo)) {
            throw Resp.camposInvalidos();
        }
        if (spotDao.existeNome(nome)) {
            throw new RespostaException(409, MSG_NOME_DUPLICADO);
        }
        try {
            spotDao.inserir(nome, andar, tipo);
        } catch (SQLException e) {
            if (SqlUtil.ehViolacaoDeIntegridade(e)) throw new RespostaException(409, MSG_NOME_DUPLICADO);
            throw e;
        }
        return Resp.criar(201, "Vaga criada com sucesso", null);
    }

    /** getvagas (user e admin): todas as vagas. */
    public JsonObject listar(JsonObject data) throws RespostaException, SQLException {
        auth.autenticar(data);

        JsonArray lista = new JsonArray();
        for (ParkingSpot v : spotDao.listar()) {
            JsonObject o = new JsonObject();
            o.addProperty("floor", v.getFloor());
            o.addProperty("spotName", v.getSpotName());
            o.addProperty("status", v.getStatus());
            o.addProperty("licensePlate", v.getLicensePlate()); // null quando livre
            o.addProperty("type", v.getType());
            lista.add(o);
        }
        return Resp.criar(200, "Vagas obtidas com sucesso", lista);
    }

    /** deletevaga (admin). Vaga ocupada não pode ser excluída. */
    public JsonObject remover(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem excluir vagas.");

        String nome = JsonUtil.texto(data, "spotName");
        if (!Validator.spotName(nome)) throw Resp.camposInvalidos();

        ParkingSpot vaga = spotDao.buscarPorNome(nome);
        if (vaga == null) throw new RespostaException(404, MSG_VAGA_NAO_ENCONTRADA);
        if (ParkingSpot.OCUPADA.equals(vaga.getStatus())) throw new RespostaException(409, MSG_VAGA_OCUPADA);

        if (!spotDao.remover(vaga.getSpotName())) throw new RespostaException(404, MSG_VAGA_NAO_ENCONTRADA);
        return Resp.criar(200, "Vaga removida com sucesso", null);
    }

    /**
     * updatevaga.
     *  admin: newSpotName, floor, status (e licensePlate para registrar/limpar o veículo);
     *  user : status e licensePlate (estacionar o próprio veículo / liberar a própria vaga).
     * Campos omitidos preservam o valor atual; licensePlate:null desvincula e deixa a vaga "disponivel".
     */
    public JsonObject atualizar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);

        String nome = JsonUtil.texto(data, "spotName");
        if (!Validator.spotName(nome)) throw Resp.camposInvalidos();

        boolean temNovoNome = data.has("newSpotName");
        boolean temAndar = data.has("floor");
        boolean temStatus = data.has("status");
        boolean temPlaca = data.has("licensePlate");

        String novoNome = temNovoNome ? JsonUtil.texto(data, "newSpotName") : null;
        Integer novoAndar = temAndar ? JsonUtil.inteiro(data, "floor") : null;
        String status = temStatus ? JsonUtil.texto(data, "status") : null;
        boolean placaNula = temPlaca && JsonUtil.ehNulo(data, "licensePlate");
        String placa = temPlaca && !placaNula ? JsonUtil.texto(data, "licensePlate") : null;

        if ((temNovoNome && !Validator.spotName(novoNome))
                || (temAndar && novoAndar == null)
                || (temStatus && !Validator.spotStatus(status))
                || (temPlaca && !placaNula && !Validator.plate(placa))) {
            throw Resp.camposInvalidos();
        }

        ParkingSpot atual = spotDao.buscarPorNome(nome);
        if (atual == null) throw new RespostaException(404, MSG_VAGA_NAO_ENCONTRADA);

        String nomeFinal = atual.getSpotName();
        int andarFinal = atual.getFloor();
        String statusFinal = atual.getStatus();
        String placaFinal = atual.getLicensePlate();

        // --- renomear / mudar de andar: só admin e só com a vaga disponível ou fora de serviço ---
        if (temNovoNome || temAndar) {
            auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem alterar nome e andar da vaga.");
            if (ParkingSpot.OCUPADA.equals(atual.getStatus())) throw new RespostaException(409, MSG_VAGA_OCUPADA);

            if (temNovoNome && !novoNome.equalsIgnoreCase(atual.getSpotName()) && spotDao.existeNome(novoNome)) {
                throw new RespostaException(409, MSG_NOME_DUPLICADO);
            }
            if (temNovoNome) nomeFinal = novoNome;
            if (temAndar) andarFinal = novoAndar;
        }

        // --- status / veículo ---
        if (temStatus || temPlaca) {
            String destino = temStatus ? status : (placaNula ? ParkingSpot.DISPONIVEL : ParkingSpot.OCUPADA);
            boolean ocupar = ParkingSpot.OCUPADA.equals(destino);

            // coerência entre status e licensePlate
            if (ocupar && (!temPlaca || placaNula)) throw Resp.camposInvalidos();
            if (!ocupar && temPlaca && !placaNula) throw Resp.camposInvalidos();
            if (placaNula && !ParkingSpot.DISPONIVEL.equals(destino)) throw Resp.camposInvalidos();

            if (ParkingSpot.DISPONIVEL.equals(destino)) {
                if (ParkingSpot.OCUPADA.equals(atual.getStatus())) {
                    // só quem estacionou (dono do veículo) ou um admin libera
                    if (!ctx.isAdmin()) {
                        Vehicle v = atual.getLicensePlate() == null ? null : vehicleDao.buscarPorPlaca(atual.getLicensePlate());
                        if (v == null || !ctx.getUsername().equals(v.getOwnerUsername())) {
                            throw new RespostaException(401,
                                    "Operação não autorizada. Somente quem estacionou o veículo ou um administrador pode liberar a vaga.");
                        }
                    }
                } else if (ParkingSpot.FORA_DE_SERVICO.equals(atual.getStatus())) {
                    auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem reativar vagas fora de serviço.");
                }
                statusFinal = ParkingSpot.DISPONIVEL;
                placaFinal = null;

            } else if (ocupar) {
                if (!ParkingSpot.DISPONIVEL.equals(atual.getStatus())) {
                    throw new RespostaException(409, "Vaga indisponível. Somente vagas disponíveis podem ser ocupadas.");
                }
                Vehicle v = vehicleDao.buscarPorPlaca(placa);
                if (v == null || (!ctx.isAdmin() && !ctx.getUsername().equals(v.getOwnerUsername()))) {
                    throw new RespostaException(404, "Veículo não encontrado.");
                }
                if (!v.getType().equals(atual.getType())) {
                    throw new RespostaException(409, "Tipo do veículo incompatível com o tipo da vaga.");
                }
                if (spotDao.buscarPorPlaca(v.getLicensePlate()) != null) {
                    throw new RespostaException(409, "Veículo já está estacionado em uma vaga.");
                }
                statusFinal = ParkingSpot.OCUPADA;
                placaFinal = v.getLicensePlate();

            } else { // fora_de_servico
                auth.exigirAdmin(ctx, "Operação não autorizada. Apenas administradores podem colocar vagas fora de serviço.");
                if (ParkingSpot.OCUPADA.equals(atual.getStatus())) throw new RespostaException(409, MSG_VAGA_OCUPADA);
                statusFinal = ParkingSpot.FORA_DE_SERVICO;
                placaFinal = null;
            }
        }

        try {
            if (!spotDao.atualizar(atual.getSpotName(), nomeFinal, andarFinal, statusFinal, placaFinal)) {
                throw new RespostaException(404, MSG_VAGA_NAO_ENCONTRADA);
            }
        } catch (SQLException e) {
            if (SqlUtil.ehViolacaoDeIntegridade(e)) throw new RespostaException(409, MSG_NOME_DUPLICADO);
            throw e;
        }
        return Resp.criar(200, "Vaga atualizada com sucesso", null);
    }
}
