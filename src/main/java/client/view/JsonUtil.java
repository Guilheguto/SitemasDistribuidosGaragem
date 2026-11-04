package client.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class JsonUtil {
    private JsonUtil() {}

    static String texto(JsonObject o, String chave) {
        JsonElement el = o.get(chave);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) return null;
        return el.getAsString();
    }

    static Integer inteiro(JsonObject o, String chave) {
        JsonElement el = o.get(chave);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) return null;
        try {
            return el.getAsInt();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
