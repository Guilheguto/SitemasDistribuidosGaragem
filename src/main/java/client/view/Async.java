package client.view;

import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Executa a chamada de rede fora da thread da interface e entrega a resposta de volta nela. */
final class Async {
    private Async() {}

    static void executar(Component parent, Callable<JsonObject> chamada, Consumer<JsonObject> aoResponder) {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                return chamada.call();
            }

            @Override
            protected void done() {
                JsonObject resp;
                try {
                    resp = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(parent, "Erro de conexão: " + causa.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                aoResponder.accept(resp);
            }
        }.execute();
    }
}
