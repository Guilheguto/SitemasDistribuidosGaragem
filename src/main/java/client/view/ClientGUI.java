package client.view;

import client.controller.ClientController;
import client.model.UserSession;
import client.network.NetworkService;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientGUI extends JFrame {

    private static final String LOGIN = "LOGIN";
    private static final String CADASTRO = "CADASTRO";
    private static final String DASHBOARD = "DASHBOARD";
    private static final String ADMIN_MENU = "ADMIN_MENU";
    private static final String ADMIN_STATUS = "ADMIN_STATUS";
    private static final String ADMIN_USUARIOS = "ADMIN_USUARIOS";
    private static final String VAGAS = "VAGAS";
    private static final String VEICULOS = "VEICULOS";

    private final NetworkService networkService = new NetworkService("127.0.0.1", 20000);
    private final ClientController controller = new ClientController(networkService);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);
    private final JPanel painelConexao = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
    private final JTextField txtIp = new JTextField("127.0.0.1", 14);
    private final JTextField txtPorta = new JTextField("20000", 6);
    //private final MessageLogPanel logPanel = new MessageLogPanel();

    private final JTextField txtLoginUser = new JTextField(15);
    private final JPasswordField txtLoginPass = new JPasswordField(15);
    private final JTextField txtRegName = new JTextField(15);
    private final JTextField txtRegUser = new JTextField(15);
    private final JPasswordField txtRegPass = new JPasswordField(15);

    private final JLabel lblWelcome = new JLabel("Bem-vindo!", SwingConstants.CENTER);
    private final JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private final AdminUsersPanel adminUsersPanel;
    private final AdminMenuPanel adminMenuPanel;
    private final UserManagementPanel userManagementPanel;
    private final SpotsPanel spotsPanel;
    private final VehiclesPanel vehiclesPanel;

    public ClientGUI() {
        setTitle("Sistema Distribuído - Cliente (Estacionamento)");
        setSize(860, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // encerra a sessão no servidor para não deixar o usuário ONLINE para sempre
                controller.logout();
                dispose();
                System.exit(0);
            }
        });

        //networkService.setLogListener(logPanel::adicionar);

        painelConexao.setBorder(BorderFactory.createTitledBorder("Servidor de destino"));
        painelConexao.add(new JLabel("IP:"));
        painelConexao.add(txtIp);
        painelConexao.add(new JLabel("Porta:"));
        painelConexao.add(txtPorta);

        Runnable aoExpirarSessao = this::voltarParaLogin;

        adminUsersPanel = new AdminUsersPanel(controller, () -> mostrar(ADMIN_MENU), aoExpirarSessao);
        userManagementPanel = new UserManagementPanel(controller, () -> mostrar(ADMIN_MENU), aoExpirarSessao);
        spotsPanel = new SpotsPanel(controller, this::voltarDasVagas, aoExpirarSessao);
        vehiclesPanel = new VehiclesPanel(controller, () -> mostrar(DASHBOARD), aoExpirarSessao);

        adminMenuPanel = new AdminMenuPanel(
                () -> { userManagementPanel.abrir(); mostrar(ADMIN_USUARIOS); },
                this::abrirVagas,
                this::localizarVeiculo,
                () -> { adminUsersPanel.iniciarMonitoramento(); mostrar(ADMIN_STATUS); },
                () -> mostrar(DASHBOARD)
        );

        mainPanel.add(criarPainelLogin(), LOGIN);
        mainPanel.add(criarPainelCadastro(), CADASTRO);
        mainPanel.add(criarPainelDashboard(), DASHBOARD);
        mainPanel.add(adminMenuPanel, ADMIN_MENU);
        mainPanel.add(adminUsersPanel, ADMIN_STATUS);
        mainPanel.add(userManagementPanel, ADMIN_USUARIOS);
        mainPanel.add(spotsPanel, VAGAS);
        mainPanel.add(vehiclesPanel, VEICULOS);

        setLayout(new BorderLayout());
        add(painelConexao, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        //add(logPanel, BorderLayout.SOUTH);

        mostrar(LOGIN);
    }

    // ------------------------ navegação ------------------------

    private void mostrar(String card) {
        if (!ADMIN_STATUS.equals(card)) adminUsersPanel.pararMonitoramento();
        painelConexao.setVisible(LOGIN.equals(card) || CADASTRO.equals(card)); // IP/porta só antes do login
        cardLayout.show(mainPanel, card);
    }

    private void abrirVagas() {
        spotsPanel.abrir(controller.getSession().isAdmin());
        mostrar(VAGAS);
    }

    private void voltarDasVagas() {
        mostrar(controller.getSession().isAdmin() ? ADMIN_MENU : DASHBOARD);
    }

    private void voltarParaLogin() {
        adminUsersPanel.pararMonitoramento();
        controller.getSession().clear();
        txtLoginUser.setText("");
        txtLoginPass.setText("");
        mostrar(LOGIN);
    }

    // ------------------------ conexão ------------------------

    /** Aplica o IP e a porta digitados ao serviço de rede. */
    private boolean aplicarConexao() {
        String ip = txtIp.getText().trim();
        if (ip.isEmpty()) {
            Forms.aviso(this, "Informe o IP do servidor.");
            return false;
        }
        Integer porta = Forms.inteiro(txtPorta.getText());
        if (porta == null || porta < 1 || porta > 65535) {
            Forms.aviso(this, "Porta inválida! Informe um número entre 1 e 65535.");
            return false;
        }
        networkService.setServerIp(ip);
        networkService.setServerPort(porta);
        return true;
    }

    // ------------------------ telas ------------------------

    private JPanel criarPainelLogin() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("LOGIN", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));

        JButton btnLogin = new JButton("Entrar");
        JButton btnGoRegister = new JButton("Criar Conta");
        btnLogin.addActionListener(e -> executarLogin());
        btnGoRegister.addActionListener(e -> mostrar(CADASTRO));
        txtLoginPass.addActionListener(e -> executarLogin());

        JPanel botoes = new JPanel(new GridLayout(1, 2, 10, 0));
        botoes.add(btnLogin);
        botoes.add(btnGoRegister);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(10, 8, 20, 8);
        panel.add(lblTitulo, gbc);
        gbc.gridwidth = 1; gbc.insets = new Insets(6, 8, 6, 8);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(txtLoginUser, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(txtLoginPass, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(15, 8, 5, 8);
        panel.add(botoes, gbc);
        return panel;
    }

    private JPanel criarPainelCadastro() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("Novo Cadastro", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));

        JButton btnRegister = new JButton("Cadastrar");
        JButton btnBack = new JButton("Voltar");
        btnRegister.addActionListener(e -> executarCadastro());
        btnBack.addActionListener(e -> mostrar(LOGIN));

        JPanel botoes = new JPanel(new GridLayout(1, 2, 10, 0));
        botoes.add(btnRegister);
        botoes.add(btnBack);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(10, 8, 20, 8);
        panel.add(lblTitulo, gbc);
        gbc.gridwidth = 1; gbc.insets = new Insets(6, 8, 6, 8);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(txtRegName, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(txtRegUser, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(txtRegPass, gbc);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.insets = new Insets(15, 8, 8, 8);
        panel.add(botoes, gbc);
        return panel;
    }

    private JPanel criarPainelDashboard() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 60, 15, 60));
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 15));
        panel.add(lblWelcome, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        return panel;
    }

    /** Monta os botões conforme o perfil (a verificação real de permissão é sempre do servidor). */
    private void configurarDashboard() {
        UserSession s = controller.getSession();
        boolean admin = s.isAdmin();

        lblWelcome.setText("Usuário: " + s.getUsername() + " (" + (admin ? "admin" : "user") + ")  |  Servidor: "
                + networkService.getServerIp() + ":" + networkService.getServerPort());

        buttonsPanel.removeAll();
        buttonsPanel.add(botao("Buscar Meus Dados", this::buscarMeusDados));
        buttonsPanel.add(botao("Alterar Nome", this::alterarNome));
        buttonsPanel.add(botao("Alterar Senha", this::alterarSenha));
        if (admin) {
            buttonsPanel.add(botao("Painel Admin", () -> mostrar(ADMIN_MENU)));
        } else {
            buttonsPanel.add(botao("Meus Veículos", () -> { vehiclesPanel.abrir(); mostrar(VEICULOS); }));
            buttonsPanel.add(botao("Vagas (listar / estacionar / liberar)", this::abrirVagas));
            buttonsPanel.add(botao("Localizar Meu Veículo", this::localizarVeiculo));
            buttonsPanel.add(botao("Excluir Conta", this::excluirConta));
        }
        buttonsPanel.add(botao("Sair / Logout", this::executarLogout));
        buttonsPanel.revalidate();
        buttonsPanel.repaint();
    }

    private JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> acao.run());
        return b;
    }

    // ------------------------ ações ------------------------

    private void executarLogin() {
        if (!aplicarConexao()) return;
        final String user = txtLoginUser.getText().trim();
        final String pass = new String(txtLoginPass.getPassword());

        Async.executar(this, () -> controller.login(user, pass), resp -> {
            if (ResponseHandler.processarResposta(this, resp, null)) {
                txtLoginPass.setText("");
                configurarDashboard();
                mostrar(DASHBOARD);
            }
        });
    }

    private void executarCadastro() {
        if (!aplicarConexao()) return;
        final String name = txtRegName.getText().trim();
        final String user = txtRegUser.getText().trim();
        final String pass = new String(txtRegPass.getPassword());

        Async.executar(this, () -> controller.cadastrar(name, user, pass), resp -> {
            if (ResponseHandler.processarResposta(this, resp, "Usuário cadastrado com sucesso!")) {
                txtRegName.setText("");
                txtRegUser.setText("");
                txtRegPass.setText("");
                mostrar(LOGIN);
            }
        });
    }

    private void buscarMeusDados() {
        Async.executar(this, controller::buscarMeusDados, resp -> {
            if (!ResponseHandler.processarResposta(this, resp, null, this::voltarParaLogin)) return;

            JsonObject d = resp.getAsJsonObject("data");
            Object[][] dados = {{JsonUtil.texto(d, "name"), JsonUtil.texto(d, "username"), JsonUtil.texto(d, "role")}};
            DefaultTableModel model = new DefaultTableModel(dados, new String[]{"Nome", "Usuário", "Perfil"}) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable table = new JTable(model);
            table.setPreferredScrollableViewportSize(new Dimension(360, 50));
            JOptionPane.showMessageDialog(this, new JScrollPane(table), "Meus Dados Cadastrais",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void alterarNome() {
        String atual = controller.getSession().getName();
        String novo = JOptionPane.showInputDialog(this, "Digite o novo nome:", atual);
        if (novo == null || novo.trim().isEmpty()) return;
        final String novoNome = novo.trim();

        Async.executar(this, () -> controller.alterarNome(novoNome), resp ->
                ResponseHandler.processarResposta(this, resp, "Nome atualizado com sucesso!", this::voltarParaLogin));
    }

    private void alterarSenha() {
        JPasswordField txtOld = new JPasswordField(15);
        JPasswordField txtNew = new JPasswordField(15);
        if (!Forms.mostrar(this, "Alterar Senha",
                new String[]{"Senha atual:", "Nova senha (8-20, maiúscula, minúscula, número e símbolo):"},
                new JComponent[]{txtOld, txtNew})) return;

        final String oldPass = new String(txtOld.getPassword());
        final String newPass = new String(txtNew.getPassword());
        if (oldPass.isEmpty() || newPass.isEmpty()) return;

        Async.executar(this, () -> controller.alterarSenha(oldPass, newPass), resp ->
                ResponseHandler.processarResposta(this, resp, "Senha atualizada com sucesso!", this::voltarParaLogin));
    }

    private void excluirConta() {
        int opcao = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja excluir sua conta? Seus veículos também serão excluídos e a ação não poderá ser desfeita.",
                "Confirmar Exclusão de Conta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcao != JOptionPane.YES_OPTION) return;

        Async.executar(this, controller::excluirConta, resp -> {
            if (ResponseHandler.processarResposta(this, resp, "Sua conta foi excluída com sucesso.", this::voltarParaLogin)) {
                voltarParaLogin();
            }
        });
    }

    /** findvehicle: user localiza o próprio veículo; admin, qualquer um. */
    private void localizarVeiculo() {
        String placa = JOptionPane.showInputDialog(this, "Placa do veículo (7 caracteres):", "Localizar Veículo",
                JOptionPane.QUESTION_MESSAGE);
        if (placa == null || placa.trim().isEmpty()) return;
        final String p = placa.trim();

        Async.executar(this, () -> controller.localizarVeiculo(p), resp -> {
            if (!ResponseHandler.processarResposta(this, resp, null, this::voltarParaLogin)) return;
            JsonObject d = resp.getAsJsonObject("data");
            JOptionPane.showMessageDialog(this,
                    "Placa: " + p + "\nAndar: " + JsonUtil.inteiro(d, "floor") + "\nVaga: " + JsonUtil.texto(d, "spotName")
                            + "\nStatus: " + JsonUtil.texto(d, "status"),
                    "Veículo localizado", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void executarLogout() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                controller.logout();
                return null;
            }

            @Override
            protected void done() {
                voltarParaLogin();
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
