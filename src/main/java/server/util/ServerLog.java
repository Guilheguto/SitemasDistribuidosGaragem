package server.util;

import java.util.function.Consumer;

/** Log do servidor: escreve no console e, se houver, na tela do servidor. */
public final class ServerLog {

    private static volatile Consumer<String> ouvinte;

    private ServerLog() {}

    public static void definirOuvinte(Consumer<String> novoOuvinte) {
        ouvinte = novoOuvinte;
    }

    public static void log(String mensagem) {
        System.out.println(mensagem);
        Consumer<String> o = ouvinte;
        if (o != null) o.accept(mensagem);
    }
}
