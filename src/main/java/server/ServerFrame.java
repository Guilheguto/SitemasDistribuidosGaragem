package server;

import server.database.DatabaseInitializer;
import server.network.ClientHandler;
import server.service.RequestRouter;
import server.util.ServerLog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

/** Tela do servidor: campo para a porta, botão iniciar/parar e log de mensagens enviadas/recebidas. */
public class ServerFrame extends JFrame {

    private static final int PORTA_MINIMA = 20000;
    private static final int PORTA_MAXIMA = 25000;

    private final JTextField txtPorta = new JTextField("20000", 6);
    private final JButton btnIniciar = new JButton("Iniciar servidor");
    private final JLabel lblStatus = new JLabel("Servidor parado");
    private final JTextArea txtLog = new JTextArea();

    private volatile ServerSocket serverSocket;

    public ServerFrame(String portaInicial) {
        super("Sistema Distribuído - Servidor");
        setSize(780, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (portaInicial != null) txtPorta.setText(portaInicial);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topo.add(new JLabel("Porta (20000 a 25000):"));
        topo.add(txtPorta);
        topo.add(btnIniciar);
        topo.add(lblStatus);

        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtLog.setLineWrap(true);

        JButton btnLimpar = new JButton("Limpar log");
        btnLimpar.addActionListener(e -> txtLog.setText(""));
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sul.add(btnLimpar);

        setLayout(new BorderLayout());
        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(txtLog), BorderLayout.CENTER);
        add(sul, BorderLayout.SOUTH);

        ServerLog.definirOuvinte(msg -> SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        }));

        btnIniciar.addActionListener(e -> {
            if (serverSocket == null) iniciar(); else parar();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                parar();
                dispose();
                System.exit(0);
            }
        });
    }

    private void iniciar() {
        int porta;
        try {
            porta = Integer.parseInt(txtPorta.getText().trim());
        } catch (NumberFormatException e) {
            erro("Informe apenas números inteiros entre " + PORTA_MINIMA + " e " + PORTA_MAXIMA + ".");
            return;
        }
        if (porta < PORTA_MINIMA || porta > PORTA_MAXIMA) {
            erro("Porta inválida! Use um valor entre " + PORTA_MINIMA + " e " + PORTA_MAXIMA + ".");
            return;
        }

        try {
            DatabaseInitializer.inicializar(); // cria tabelas que faltarem e a conta admin inicial
        } catch (SQLException e) {
            erro("Não foi possível preparar o banco de dados:\n" + e.getMessage());
            return;
        }

        final ServerSocket ss;
        try {
            ss = new ServerSocket(porta);
        } catch (IOException e) {
            erro("Não foi possível abrir a porta " + porta + ":\n" + e.getMessage());
            return;
        }
        serverSocket = ss;

        final RequestRouter router = new RequestRouter();
        Thread aceitador = new Thread(() -> {
            while (!ss.isClosed()) {
                try {
                    Socket cliente = ss.accept();
                    new Thread(new ClientHandler(cliente, router)).start();
                } catch (IOException e) {
                    if (!ss.isClosed()) ServerLog.log("[Servidor] Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }, "aceitador-de-conexoes");
        aceitador.setDaemon(true);
        aceitador.start();

        ServerLog.log("Servidor iniciado na porta " + porta);
        lblStatus.setText("Ouvindo na porta " + porta);
        btnIniciar.setText("Parar servidor");
        txtPorta.setEnabled(false);
    }

    private void parar() {
        ServerSocket ss = serverSocket;
        if (ss == null) return;
        serverSocket = null;
        try {
            ss.close();
        } catch (IOException ignored) {}
        ServerLog.log("Servidor encerrado");
        lblStatus.setText("Servidor parado");
        btnIniciar.setText("Iniciar servidor");
        txtPorta.setEnabled(true);
    }

    private void erro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
