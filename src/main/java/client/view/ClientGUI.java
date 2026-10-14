package client.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import client.controller.ClientController;
import client.network.NetworkService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ClientGUI extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private ClientController controller;

    private JTextField txtLoginUser;
    private JPasswordField txtLoginPass;

    private JTextField txtRegName;
    private JTextField txtRegUser;
    private JPasswordField txtRegPass;

    private JLabel lblWelcome;

    private AdminUsersPanel adminUsersPanel;
    private AdminMenuPanel adminMenuPanel;

    public ClientGUI() {
        setTitle("Sistema Distribuído - Cliente");
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // encerra a sessão no servidor, senão o usuário fica ONLINE para sempre
                if (controller != null && controller.getSession().getToken() != null) {
                    controller.logout();
                }
                dispose();
                System.exit(0);
            }
        });
        setLocationRelativeTo(null);

        if (!configurarConexao()) {
            System.exit(0);
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);


        mainPanel.add(criarPainelLogin(), "LOGIN");
        mainPanel.add(criarPainelCadastro(), "CADASTRO");
        mainPanel.add(criarPainelDashboard(), "DASHBOARD");


        adminUsersPanel = new AdminUsersPanel(
                controller,
                () -> cardLayout.show(mainPanel, "ADMIN_MENU"),
                this::voltarParaLogin
        );

        adminMenuPanel = new AdminMenuPanel(
                () -> {
                    adminUsersPanel.iniciarMonitoramento();
                    cardLayout.show(mainPanel, "ADMIN_USERS");
                },
                () -> cardLayout.show(mainPanel, "DASHBOARD")
        );

        mainPanel.add(adminMenuPanel, "ADMIN_MENU");
        mainPanel.add(adminUsersPanel, "ADMIN_USERS");

        add(mainPanel);
    }

    private boolean configurarConexao() {
        String serverIp = JOptionPane.showInputDialog(this, "Informe o IP do Servidor:", "127.0.0.1");
        if (serverIp == null || serverIp.trim().isEmpty()) return false;

        String portStr = JOptionPane.showInputDialog(this, "Informe a Porta do Servidor (20000-25000):", "20000");
        if (portStr == null || portStr.trim().isEmpty()) return false;

        try {
            int serverPort = Integer.parseInt(portStr.trim());
            if (this.controller == null) {
                this.controller = new ClientController(new NetworkService(serverIp.trim(), serverPort));
            } else {
                // reaproveita o controller: os painéis já guardam a referência dele
                controller.getNetworkService().setServerIp(serverIp.trim());
                controller.getNetworkService().setServerPort(serverPort);
            }
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JPanel criarPainelLogin() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("LOGIN", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));

        txtLoginUser = new JTextField(15);
        txtLoginPass = new JPasswordField(15);

        JButton btnLogin = new JButton("Entrar");
        JButton btnGoRegister = new JButton("Criar Conta");
        JButton btnCancel = new JButton("Cancelar");

        btnLogin.addActionListener(e -> executarLogin());
        btnGoRegister.addActionListener(e -> cardLayout.show(mainPanel, "CADASTRO"));
        btnCancel.addActionListener(e -> {
            txtLoginUser.setText("");
            txtLoginPass.setText("");
            configurarConexao();
        });

        JPanel painelBotoesLogReg = new JPanel(new GridLayout(1, 2, 10, 0));
        painelBotoesLogReg.add(btnLogin);
        painelBotoesLogReg.add(btnGoRegister);

        JPanel panelBotaoCancel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnCancel.setPreferredSize(new Dimension(150, 26));
        panelBotaoCancel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 8, 20, 8);
        panel.add(lblTitulo, gbc);

        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(txtLoginUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(txtLoginPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 5, 8);
        panel.add(painelBotoesLogReg, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 8, 8, 8);
        panel.add(panelBotaoCancel, gbc);

        return panel;
    }

    private JPanel criarPainelCadastro() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("Novo Cadastro", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));

        txtRegName = new JTextField(15);
        txtRegUser = new JTextField(15);
        txtRegPass = new JPasswordField(15);

        JButton btnRegister = new JButton("Cadastrar");
        JButton btnBack = new JButton("Voltar");

        btnRegister.addActionListener(e -> executarCadastro());
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        JPanel panelBotoesCadBack = new JPanel(new GridLayout(1, 2, 10, 0));
        panelBotoesCadBack.add(btnRegister);
        panelBotoesCadBack.add(btnBack);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 8, 20, 8);
        panel.add(lblTitulo, gbc);

        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(txtRegName, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(txtRegUser, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(txtRegPass, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        panel.add(panelBotoesCadBack, gbc);

        return panel;
    }

    private JPanel criarPainelDashboard() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        lblWelcome = new JLabel("Bem-vindo!", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel buttonsPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        JButton btnBuscarDados = new JButton("Buscar Meus Dados");
        JButton btnAlterarSenha = new JButton("Alterar Senha");
        JButton btnAlterarNome = new JButton("Alterar Nome");
        JButton btnExcluirConta = new JButton("Excluir Conta");
        JButton btnLogout = new JButton("Sair / Logout");
        JButton btnPainelAdmin = new JButton("Painel Admin");

        btnBuscarDados.addActionListener(e -> buscarMeusDados());
        btnAlterarSenha.addActionListener(e -> alterarSenha());
        btnAlterarNome.addActionListener(e -> alterarNome());
        btnExcluirConta.addActionListener(e -> excluirConta());
        btnLogout.addActionListener(e -> executarLogout());

        // Direciona diretamente para o menu principal do admin no CardLayout
        btnPainelAdmin.addActionListener(e -> cardLayout.show(mainPanel, "ADMIN_MENU"));

        buttonsPanel.add(btnBuscarDados);
        buttonsPanel.add(btnAlterarSenha);
        buttonsPanel.add(btnAlterarNome);
        buttonsPanel.add(btnExcluirConta);
        buttonsPanel.add(btnLogout);
        buttonsPanel.add(btnPainelAdmin);

        panel.add(lblWelcome, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);

        return panel;
    }

    private void voltarParaLogin() {
        if (adminUsersPanel != null) {
            adminUsersPanel.pararMonitoramento();
        }
        if (controller != null && controller.getSession() != null) {
            controller.getSession().clear();
        }
        txtLoginUser.setText("");
        txtLoginPass.setText("");
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void erroDeConexao(Exception e) {
        JOptionPane.showMessageDialog(this, "Erro de conexão: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void executarLogin() {
        try {
            String user = txtLoginUser.getText().trim();
            String pass = new String(txtLoginPass.getPassword());

            JsonObject resp = controller.login(user, pass);

            if (ResponseHandler.processarResposta(this, resp, null)) {
                lblWelcome.setText("Usuário conectado: " + user);
                txtLoginPass.setText("");
                cardLayout.show(mainPanel, "DASHBOARD");
            }
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void executarCadastro() {
        try {
            String name = txtRegName.getText().trim();
            String user = txtRegUser.getText().trim();
            String pass = new String(txtRegPass.getPassword());

            JsonObject resp = controller.cadastrar(name, user, pass);

            if (ResponseHandler.processarResposta(this, resp, "Usuário cadastrado com sucesso!")) {
                txtRegName.setText("");
                txtRegUser.setText("");
                txtRegPass.setText("");
                cardLayout.show(mainPanel, "LOGIN");
            }
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void alterarSenha() {
        JPasswordField txtOld = new JPasswordField(15);
        JPasswordField txtNew = new JPasswordField(15);

        JPanel painel = new JPanel(new GridLayout(4, 1, 5, 5));
        painel.add(new JLabel("Senha atual:"));
        painel.add(txtOld);
        painel.add(new JLabel("Nova senha (8-20 caracteres, com maiúscula,,minúscula, número e símbolo):"));
        painel.add(txtNew);
        int opcao = JOptionPane.showConfirmDialog(this, painel, "Alterar Senha", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return;

        String oldPass = new String(txtOld.getPassword());
        String newPass = new String(txtNew.getPassword());
        if (oldPass.isEmpty() || newPass.isEmpty()) return;

        try {
            JsonObject resp = controller.alterarSenha(oldPass, newPass);
            ResponseHandler.processarResposta(this, resp, "Senha atualizada com sucesso!");
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void buscarMeusDados() {
        try {
            JsonObject resp = controller.buscarMeusDados();

            if (!ResponseHandler.processarResposta(this, resp, null, this::voltarParaLogin)) return;

            JsonObject userData = resp.getAsJsonObject("data");
            String name = userData.get("name").getAsString();
            String username = userData.get("username").getAsString();
            String creationDate = userData.has("creationDate") && !userData.get("creationDate").isJsonNull()
                    ? userData.get("creationDate").getAsString()
                    : "-";

            String[] colunas = {"Nome", "Usuário", "Data Criação"};
            Object[][] dados = {{name, username, creationDate}};

            DefaultTableModel model = new DefaultTableModel(dados, colunas) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable table = new JTable(model);
            table.setPreferredScrollableViewportSize(new Dimension(300, 50));
            JScrollPane scrollPane = new JScrollPane(table);

            JOptionPane.showMessageDialog(this, scrollPane, "Meus Dados Cadastrais", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void alterarNome() {
        String nameAtual = controller.getSession() != null ? controller.getSession().getName() : "";
        String newName = JOptionPane.showInputDialog(this, "Digite o novo nome:", nameAtual);
        if (newName == null || newName.trim().isEmpty()) return;

        try {
            JsonObject resp = controller.alterarNome(newName.trim());
            ResponseHandler.processarResposta(this, resp, "Nome atualizado com sucesso!", this::voltarParaLogin);
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void excluirConta() {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja excluir sua conta? Esta ação não poderá ser desfeita.",
                "Confirmar Exclusão de Conta",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (opcao != JOptionPane.YES_OPTION) return;

        try {
            JsonObject resp = controller.excluirConta();

            if (ResponseHandler.processarResposta(this, resp, "Sua conta foi excluída com sucesso.", this::voltarParaLogin)) {
                voltarParaLogin();
            }
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void listarUsuariosLogados() {
        try {
            JsonObject resp = controller.listarUsuariosLogados();

            if (!ResponseHandler.processarResposta(this, resp, null, this::voltarParaLogin)) return;

            JsonArray users = resp.getAsJsonObject("data").getAsJsonArray("users");
            DefaultListModel model = new DefaultListModel<>();
            for (JsonElement u : users) {
                model.addElement(u.getAsString());
            }

            JScrollPane scroll = new JScrollPane(new JList<>(model));
            scroll.setPreferredSize(new Dimension(250, 150));

            JOptionPane.showMessageDialog(this, scroll,
                    "Usuários Logados (" + model.size() + ")", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            erroDeConexao(e);
        }
    }

    private void executarLogout() {
        try {
            controller.logout();
        } catch (Exception ignored) {
        } finally {
            voltarParaLogin();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}