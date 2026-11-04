package server.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

/** Leitura segura de campos: nunca lança exceção por tipo errado. */
public final class JsonUtil {
    private JsonUtil() {}

    /** String do campo, ou null se ausente, null ou de outro tipo. */
    public static String texto(JsonObject o, String chave) {
        JsonElement el = o.get(chave);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) return null;
        return el.getAsString();
    }

    /** Inteiro do campo, ou null se ausente, null, não numérico ou fracionário. */
    public static Integer inteiro(JsonObject o, String chave) {
        JsonElement el = o.get(chave);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) return null;
        try {
            BigDecimal valor = el.getAsBigDecimal();
            return valor.intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /** O campo existe no JSON e vale explicitamente null? */
    public static boolean ehNulo(JsonObject o, String chave) {
        JsonElement el = o.get(chave);
        return el != null && el.isJsonNull();
    }
}
