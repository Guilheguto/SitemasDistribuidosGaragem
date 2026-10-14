package server;

import server.database.UserDao;
import server.network.ClientHandler;
import server.service.RequestRouter;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORTA_MINIMA = 20000;
    private static final int PORTA_MAXIMA = 25000;
    private static final int PORTA_PADRAO = 20000;

    public static void main(String[] args) {
        int porta = obterPorta(args);
        RequestRouter router = new RequestRouter();
        new UserDao().limparSessoes();

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[SERVIDOR] Ouvindo na porta " + porta);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, router)).start();
            }
        } catch (IOException e) {
            System.err.println("[ERRO CRÍTICO] " + e.getMessage());
        }
    }

    private static int obterPorta(String[] args) {
        if (args.length > 0) {
            try {
                int portaArg = Integer.parseInt(args[0]);
                if (portaArg >= PORTA_MINIMA && portaArg <= PORTA_MAXIMA) {
                    return portaArg;
                }
            } catch (NumberFormatException ignored) {}
        }

        while (true) {
            String input = JOptionPane.showInputDialog(
                    null,
                    "Informe a porta para iniciar o servidor (entre 20000 e 25000):",
                    "Configuração do Servidor",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (input == null) {
                return PORTA_PADRAO;
            }

            try {
                int porta = Integer.parseInt(input.trim());
                if (porta >= PORTA_MINIMA && porta <= PORTA_MAXIMA) {
                    return porta;
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "Porta inválida! A porta deve ser um valor no escopo de 20000 a 25000.",
                            "Erro de Validação",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Por favor, insira apenas números inteiros no escopo de 20000 a 25000.",
                        "Erro de Formato",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}