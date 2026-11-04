package client.view;

import client.controller.ClientController;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;

/**
 * Admin: consulta, altera o nome, exclui e cadastra usuários.
 * Os dados do usuário consultado ficam separados da sessão do admin
 * (consultar/excluir outra conta não altera token, username nem role do admin).
 */
public class UserManagementPanel extends JPanel {

    private final ClientController controller;
    private final Runnable onVoltar;
    private final Runnable onSessaoExpirada;

    private final JTextField txtBusca = new JTextField(16);
    private final JLabel lblNome = new JLabel("-");
    private final JLabel lblUsuario = new JLabel("-");
    private final JLabel lblPerfil = new JLabel("-");
    private final JButton btnAlterarNome = new JButton("Alterar Nome");
    private final JButton btnExcluir = new JButton("Excluir Usuário");

    private String usuarioConsultado; // conta administrada (independente da sessão)

    public UserManagementPanel(ClientController controller, Runnable onVoltar, Runnable onSessaoExpirada) {
        this.controller = controller;
        this.onVoltar = onVoltar;
        this.onSessaoExpirada = onSessaoExpirada;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JLabel titulo = new JLabel("Gerenciamento de Usuários", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(titulo, BorderLayout.NORTH);

        JPanel busca = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton btnConsultar = new JButton("Consultar");
        busca.add(new JLabel("Username:"));
        busca.add(txtBusca);
        busca.add(btnConsultar);

        JPanel detalhes = new JPanel(new GridLayout(3, 2, 8, 8));
        detalhes.setBorder(BorderFactory.createTitledBorder("Usuário consultado"));
        detalhes.add(new JLabel("Nome:"));    detalhes.add(lblNome);
        detalhes.add(new JLabel("Username:")); detalhes.add(lblUsuario);
        detalhes.add(new JLabel("Perfil:"));  detalhes.add(lblPerfil);

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.add(busca, BorderLayout.NORTH);
        centro.add(detalhes, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        JButton btnCadastrar = new JButton("Cadastrar Novo Usuário");
        JButton btnVoltar = new JButton("Voltar ao Painel Admin");
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        botoes.add(btnAlterarNome);
        botoes.add(btnExcluir);
        botoes.add(btnCadastrar);
        botoes.add(btnVoltar);
        add(botoes, BorderLayout.SOUTH);

        btnConsultar.addActionListener(e -> consultar());
        txtBusca.addActionListener(e -> consultar());
        btnAlterarNome.addActionListener(e -> alterarNome());
        btnExcluir.addActionListener(e -> excluir());
        btnCadastrar.addActionListener(e -> cadastrar());
        btnVoltar.addActionListener(e -> onVoltar.run());
    }

    public void abrir() {
        txtBusca.setText("");
        limpar();
    }

    private void limpar() {
        usuarioConsultado = null;
        lblNome.setText("-");
        lblUsuario.setText("-");
        lblPerfil.setText("-");
        btnAlterarNome.setEnabled(false);
        btnExcluir.setEnabled(false);
    }

    private boolean ok(JsonObject resp, String msgSucesso) {
        return ResponseHandler.processarResposta(this, resp, msgSucesso, onSessaoExpirada);
    }

    private void consultar() {
        String username = txtBusca.getText().trim();
        if (username.isEmpty()) {
            Forms.aviso(this, "Informe o username a consultar.");
            return;
        }
        Async.executar(this, () -> controller.buscarUsuario(username), resp -> {
            if (!ok(resp, null)) {
                limpar();
                return;
            }
            JsonObject d = resp.getAsJsonObject("data");
            usuarioConsultado = JsonUtil.texto(d, "username");
            lblNome.setText(String.valueOf(JsonUtil.texto(d, "name")));
            lblUsuario.setText(String.valueOf(usuarioConsultado));
            lblPerfil.setText(String.valueOf(JsonUtil.texto(d, "role")));
            btnAlterarNome.setEnabled(usuarioConsultado != null);
            btnExcluir.setEnabled(usuarioConsultado != null);
        });
    }

    private void alterarNome() {
        if (usuarioConsultado == null) return;
        final String alvo = usuarioConsultado;
        String novo = JOptionPane.showInputDialog(this, "Novo nome para '" + alvo + "':", lblNome.getText());
        if (novo == null || novo.trim().isEmpty()) return;
        final String novoNome = novo.trim();

        Async.executar(this, () -> controller.alterarNomeDeUsuario(alvo, novoNome), resp -> {
            if (ok(resp, "Nome atualizado com sucesso!")) {
                txtBusca.setText(alvo);
                consultar();
            }
        });
    }

    private void excluir() {
        if (usuarioConsultado == null) return;
        final String alvo = usuarioConsultado;
        int opcao = JOptionPane.showConfirmDialog(this,
                "Excluir definitivamente o usuário '" + alvo + "' e todos os seus veículos?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcao != JOptionPane.YES_OPTION) return;

        Async.executar(this, () -> controller.excluirUsuario(alvo), resp -> {
            if (ok(resp, "Usuário excluído com sucesso.")) limpar();
        });
    }

    private void cadastrar() {
        JTextField txtNome = new JTextField();
        JTextField txtUser = new JTextField();
        JPasswordField txtSenha = new JPasswordField();
        JComboBox<String> cmbRole = new JComboBox<>(new String[]{"user", "admin"});

        if (!Forms.mostrar(this, "Cadastrar usuário",
                new String[]{"Nome:", "Username:", "Senha:", "Perfil:"},
                new JComponent[]{txtNome, txtUser, txtSenha, cmbRole})) return;

        final String nome = txtNome.getText().trim();
        final String user = txtUser.getText().trim();
        final String senha = new String(txtSenha.getPassword());
        final String role = (String) cmbRole.getSelectedItem();

        Async.executar(this, () -> controller.cadastrarComRole(nome, user, senha, role),
                resp -> ok(resp, "Usuário cadastrado com sucesso!"));
    }
}
