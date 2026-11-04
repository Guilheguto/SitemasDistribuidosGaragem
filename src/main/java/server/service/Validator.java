package server.service;

import java.util.regex.Pattern;

/** Regras de validação de campos do protocolo. */
public final class Validator {

    private static final Pattern NAME = Pattern.compile("^[\\p{L} ]{1,60}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._]{3,20}$");
    private static final Pattern PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[#.*&%$@!()_=+-])[a-zA-Z0-9#.*&%$@!()_=+-]{8,20}$");
    private static final Pattern SPOT_NAME = Pattern.compile("^[A-Za-z0-9]{1,4}$");
    private static final Pattern PLATE = Pattern.compile("^[A-Za-z0-9]{7}$");

    private Validator() {}

    public static boolean name(String v) {
        return v != null && !v.trim().isEmpty() && NAME.matcher(v).matches();
    }

    public static boolean username(String v) {
        return v != null && USERNAME.matcher(v).matches();
    }

    public static boolean password(String v) {
        return v != null && PASSWORD.matcher(v).matches();
    }

    public static boolean role(String v) {
        return "admin".equals(v) || "user".equals(v);
    }

    public static boolean spotName(String v) {
        return v != null && SPOT_NAME.matcher(v).matches();
    }

    public static boolean plate(String v) {
        return v != null && PLATE.matcher(v).matches();
    }

    public static boolean brand(String v) {
        return v != null && !v.trim().isEmpty() && v.length() <= 20;
    }

    /** model aceita null; quando informado, no máximo 20 caracteres. */
    public static boolean model(String v) {
        return v == null || v.length() <= 20;
    }

    /** Tipo de vaga ou de veículo. */
    public static boolean type(String v) {
        return "eletrico".equals(v) || "combustao".equals(v);
    }

    public static boolean spotStatus(String v) {
        return "disponivel".equals(v) || "ocupada".equals(v) || "fora_de_servico".equals(v);
    }
}
