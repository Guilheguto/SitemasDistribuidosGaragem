package server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import server.database.SpotDao;
import server.database.SqlUtil;
import server.database.VehicleDao;
import server.model.ParkingSpot;
import server.model.Vehicle;

import java.sql.SQLException;

public class VehicleService {

    private static final String MSG_NAO_ENCONTRADO = "Veículo não encontrado.";
    private static final String MSG_ASSOCIADO_A_VAGA = "Veículo associado a uma vaga. Libere a vaga antes desta operação.";

    private final AuthService auth;
    private final VehicleDao vehicleDao = new VehicleDao();
    private final SpotDao spotDao = new SpotDao();

    public VehicleService(AuthService auth) {
        this.auth = auth;
    }

    /** createvehicle (somente role=user). */
    public JsonObject criar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirUsuarioComum(ctx);

        String placa = JsonUtil.texto(data, "licensePlate");
        String brand = JsonUtil.texto(data, "brand");
        String type = JsonUtil.texto(data, "type");
        boolean modelPresenteInvalido = data.has("model") && !JsonUtil.ehNulo(data, "model")
                && JsonUtil.texto(data, "model") == null;
        String model = JsonUtil.texto(data, "model");

        if (!Validator.plate(placa) || !Validator.brand(brand) || !Validator.type(type)
                || modelPresenteInvalido || !Validator.model(model)) {
            throw Resp.camposInvalidos();
        }
        if (vehicleDao.buscarPorPlaca(placa) != null) {
            throw new RespostaException(409, "Placa já cadastrada.");
        }
        try {
            vehicleDao.inserir(new Vehicle(placa, brand, model, type, ctx.getUsername()));
        } catch (SQLException e) {
            if (SqlUtil.ehViolacaoDeIntegridade(e)) throw new RespostaException(409, "Placa já cadastrada.");
            throw e;
        }
        return Resp.criar(201, "Veículo criado com sucesso", null);
    }

    /** getvehicles (somente role=user): apenas os veículos do próprio usuário. */
    public JsonObject listar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirUsuarioComum(ctx);

        JsonArray lista = new JsonArray();
        for (Vehicle v : vehicleDao.listarPorProprietario(ctx.getUsername())) {
            lista.add(paraJson(v));
        }
        return Resp.criar(200, "Veículos obtidos com sucesso", lista);
    }

    /** getvehicle (user: só os seus; admin: qualquer um). */
    public JsonObject buscar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        Vehicle v = buscarVisivel(ctx, lerPlaca(data));
        return Resp.criar(200, "Veículo encontrado com sucesso", paraJson(v));
    }

    /** updatevehicle (somente role=user, somente veículo próprio). */
    public JsonObject atualizar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        auth.exigirUsuarioComum(ctx);

        String placa = lerPlaca(data);
        Vehicle atual = buscarVisivel(ctx, placa);

        String brand = atual.getBrand();
        String model = atual.getModel();
        String type = atual.getType();

        if (data.has("brand")) {
            brand = JsonUtil.texto(data, "brand");
            if (!Validator.brand(brand)) throw Resp.camposInvalidos();
        }
        if (data.has("type")) {
            type = JsonUtil.texto(data, "type");
            if (!Validator.type(type)) throw Resp.camposInvalidos();
        }
        if (data.has("model")) {
            if (JsonUtil.ehNulo(data, "model")) {
                model = null; // model:null limpa o modelo
            } else {
                model = JsonUtil.texto(data, "model");
                if (model == null || !Validator.model(model)) throw Resp.camposInvalidos();
            }
        }

        if (!type.equals(atual.getType()) && spotDao.buscarPorPlaca(atual.getLicensePlate()) != null) {
            throw new RespostaException(409, MSG_ASSOCIADO_A_VAGA);
        }

        if (!vehicleDao.atualizar(atual.getLicensePlate(), brand, model, type)) {
            throw new RespostaException(404, MSG_NAO_ENCONTRADO);
        }
        return Resp.criar(200, "Veículo atualizado com sucesso", null);
    }

    /** deletevehicle (user: só os seus; admin: qualquer um). */
    public JsonObject remover(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        Vehicle v = buscarVisivel(ctx, lerPlaca(data));

        if (spotDao.buscarPorPlaca(v.getLicensePlate()) != null) {
            throw new RespostaException(409, MSG_ASSOCIADO_A_VAGA);
        }
        if (!vehicleDao.remover(v.getLicensePlate())) {
            throw new RespostaException(404, MSG_NAO_ENCONTRADO);
        }
        return Resp.criar(200, "Veículo removido com sucesso", null);
    }

    /** findvehicle (user: só os seus; admin: qualquer um): onde o veículo está estacionado. */
    public JsonObject localizar(JsonObject data) throws RespostaException, SQLException {
        AuthContext ctx = auth.autenticar(data);
        Vehicle v = buscarVisivel(ctx, lerPlaca(data));

        ParkingSpot vaga = spotDao.buscarPorPlaca(v.getLicensePlate());
        if (vaga == null) {
            throw new RespostaException(404, "Veículo não está estacionado em nenhuma vaga.");
        }
        JsonObject d = new JsonObject();
        d.addProperty("floor", vaga.getFloor());
        d.addProperty("spotName", vaga.getSpotName());
        d.addProperty("status", vaga.getStatus());
        return Resp.criar(200, "Veículo localizado com sucesso", d);
    }

    // ------------------------ auxiliares ------------------------

    private String lerPlaca(JsonObject data) throws RespostaException {
        String placa = JsonUtil.texto(data, "licensePlate");
        if (!Validator.plate(placa)) throw Resp.camposInvalidos();
        return placa;
    }

    /**
     * Veículo visível para quem pediu. Inexistente e alheio geram o MESMO 404,
     * para não revelar a existência de veículos de terceiros.
     */
    private Vehicle buscarVisivel(AuthContext ctx, String placa) throws RespostaException, SQLException {
        Vehicle v = vehicleDao.buscarPorPlaca(placa);
        if (v == null || (!ctx.isAdmin() && !ctx.getUsername().equals(v.getOwnerUsername()))) {
            throw new RespostaException(404, MSG_NAO_ENCONTRADO);
        }
        return v;
    }

    private JsonObject paraJson(Vehicle v) {
        JsonObject o = new JsonObject();
        o.addProperty("brand", v.getBrand());
        o.addProperty("model", v.getModel()); // null vira JSON null
        o.addProperty("type", v.getType());
        o.addProperty("licensePlate", v.getLicensePlate());
        return o; // ownerUsername nunca é exposto
    }
}
