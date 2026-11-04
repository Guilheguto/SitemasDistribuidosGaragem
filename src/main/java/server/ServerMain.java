package server;

import javax.swing.SwingUtilities;

public class ServerMain {

    public static void main(String[] args) {
        String porta = args.length > 0 ? args[0] : null;
        SwingUtilities.invokeLater(() -> new ServerFrame(porta).setVisible(true));
    }
}
