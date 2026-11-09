package client.view;

import client.controller.ClientController;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Vagas do estacionamento.
 *  admin: criar, editar (nome, andar, status), liberar e excluir;
 *  user : listar, estacionar o próprio veículo e liberar a própria vaga.
 */
public class SpotsPanel extends JPanel {

    private static final String[] STATUS = {"disponivel", "fora_de_servico", "ocupada"};
    private static final String[] TIPOS = {"eletrico", "combustao"};
    private static final int COL_ANDAR = 0, COL_VAGA = 1, COL_STATUS = 3, COL_PLACA = 4;

    private final ClientController controller;
    private final Runnable onSessaoExpirada;

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblInfo = new JLabel(" ");

    private final JButton btnNova = new JButton("Nova Vaga");
    private final JButton btnEditar = new JButton("Editar Vaga");
    private final JButton btnExcluir = new JButton("Excluir Vaga");
    private final JButton btnEstacionar = new JButton("Estacionar Veículo");
    private final JButton btnLiberar = new JButton("Liberar Vaga");

    public SpotsPanel(ClientController controller, Runnable onVoltar, Runnable onSessaoExpirada) {
        this.controller = controller;
        this.onSessaoExpirada = onSessaoExpirada;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Vagas do Estacionamento", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(titulo, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Andar", "Vaga", "Tipo", "Status", "Placa"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == COL_ANDAR ? Integer.class : String.class; }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnAtualizar = new JButton("Atualizar");
        JButton btnVoltar = new JButton("Voltar");
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        botoes.add(btnAtualizar);
        botoes.add(btnNova);
        botoes.add(btnEditar);
        botoes.add(btnEstacionar);
        botoes.add(btnLiberar);
        botoes.add(btnExcluir);
        botoes.add(btnVoltar);

        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel sul = new JPanel(new GridLayout(2, 1, 0, 4));
        sul.add(lblInfo);
        sul.add(botoes);
        add(sul, BorderLayout.SOUTH);

        btnAtualizar.addActionListener(e -> carregar());
        btnNova.addActionListener(e -> criar());
        btnEditar.addActionListener(e -> editar());
        btnExcluir.addActionListener(e -> excluir());
        btnEstacionar.addActionListener(e -> estacionar());
        btnLiberar.addActionListener(e -> liberar());
        btnVoltar.addActionListener(e -> onVoltar.run());
    }

    public void abrir(boolean admin) {
        btnNova.setVisible(admin);
        btnEditar.setVisible(admin);
        btnExcluir.setVisible(admin);
        btnEstacionar.setVisible(!admin);
        model.setRowCount(0);
        carregar();
    }

    private boolean ok(JsonObject resp, String msgSucesso) {
        return ResponseHandler.processarResposta(this, resp, msgSucesso, onSessaoExpirada);
    }

    private void carregar() {
        Async.executar(this, controller::listarVagas, resp -> {
            if (!ok(resp, null)) return;
            model.setRowCount(0);
            JsonElement dados = resp.get("data");
            if (dados != null && dados.isJsonArray()) {
                for (JsonElement e : dados.getAsJsonArray()) {
                    JsonObject o = e.getAsJsonObject();
                    String placa = JsonUtil.texto(o, "licensePlate");
                    model.addRow(new Object[]{
                            JsonUtil.inteiro(o, "floor"), JsonUtil.texto(o, "spotName"),
                            JsonUtil.texto(o, "type"), JsonUtil.texto(o, "status"),
                            placa == null ? "-" : placa});
                }
            }
            lblInfo.setText(model.getRowCount() + " vaga(s) cadastrada(s)");
        });
    }

    /** Linha (do modelo) da vaga selecionada, ou -1 com aviso. */
    private int selecionada() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Forms.aviso(this, "Selecione uma vaga na tabela.");
            return -1;
        }
        return table.convertRowIndexToModel(r);
    }

    private String celula(int linha, int coluna) {
        Object v = model.getValueAt(linha, coluna);
        return v == null ? "" : v.toString();
    }

    private void criar() {
        JTextField txtNome = new JTextField();
        JTextField txtAndar = new JTextField("1");
        JComboBox<String> cmbTipo = new JComboBox<>(TIPOS);
        if (!Forms.mostrar(this, "Nova vaga",
                new String[]{"Nome (1 a 4 caracteres):", "Andar (inteiro):", "Tipo:"},
                new JComponent[]{txtNome, txtAndar, cmbTipo})) return;

        final String nome = txtNome.getText().trim();
        final Integer andar = Forms.inteiro(txtAndar.getText());
        final String tipo = (String) cmbTipo.getSelectedItem();
        if (andar == null) {
            Forms.aviso(this, "O andar deve ser um número inteiro.");
            return;
        }
        Async.executar(this, () -> controller.criarVaga(nome, andar, tipo), resp -> {
            if (ok(resp, "Vaga criada com sucesso!")) carregar();
        });
    }

    private void editar() {
        int l = selecionada();
        if (l < 0) return;
        final String nomeAtual = celula(l, COL_VAGA);
        final String andarAtual = celula(l, COL_ANDAR);
        final String statusAtual = celula(l, COL_STATUS);
        String placaAtual = celula(l, COL_PLACA);
        if ("-".equals(placaAtual)) placaAtual = "";

        JTextField txtNome = new JTextField(nomeAtual);
        JTextField txtAndar = new JTextField(andarAtual);
        JComboBox<String> cmbStatus = new JComboBox<>(STATUS);
        cmbStatus.setSelectedItem(statusAtual);
        JTextField txtPlaca = new JTextField(placaAtual);

        if (!Forms.mostrar(this, "Editar vaga " + nomeAtual,
                new String[]{"Nome:", "Andar:", "Status:", "Placa (se status = ocupada):"},
                new JComponent[]{txtNome, txtAndar, cmbStatus, txtPlaca})) return;

        String novoNome = txtNome.getText().trim();
        Integer novoAndar = Forms.inteiro(txtAndar.getText());
        String novoStatus = (String) cmbStatus.getSelectedItem();
        if (novoAndar == null) {
            Forms.aviso(this, "O andar deve ser um número inteiro.");
            return;
        }

        // envia apenas o que mudou (campos omitidos preservam o valor atual no servidor)
        final JsonObject campos = new JsonObject();
        campos.addProperty("spotName", nomeAtual);
        if (!novoNome.equals(nomeAtual)) campos.addProperty("newSpotName", novoNome);
        if (!String.valueOf(novoAndar).equals(andarAtual.trim())) campos.addProperty("floor", novoAndar);
        if (!novoStatus.equals(statusAtual)) {
            campos.addProperty("status", novoStatus);
            if ("ocupada".equals(novoStatus)) campos.addProperty("licensePlate", txtPlaca.getText().trim());
        }
        if (campos.size() == 1) {
            Forms.aviso(this, "Nenhuma alteração foi feita.");
            return;
        }
        Async.executar(this, () -> controller.atualizarVaga(campos), resp -> {
            if (ok(resp, "Vaga atualizada com sucesso!")) carregar();
        });
    }

    private void excluir() {
        int l = selecionada();
        if (l < 0) return;
        final String nome = celula(l, COL_VAGA);
        int opcao = JOptionPane.showConfirmDialog(this, "Excluir a vaga '" + nome + "'?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcao != JOptionPane.YES_OPTION) return;

        Async.executar(this, () -> controller.removerVaga(nome), resp -> {
            if (ok(resp, "Vaga removida com sucesso!")) carregar();
        });
    }

    private void estacionar() {
        int l = selecionada();
        if (l < 0) return;
        final String vaga = celula(l, COL_VAGA);
        final String tipoVaga = celula(l, COL_TIPO);

        if (!"disponivel".equals(celula(l, COL_STATUS))) {
            Forms.aviso(this, "A vaga " + vaga + " não está disponível.");
            return;
        }

        Async.executar(this, controller::listarVeiculos, resp -> {
            if (!ok(resp, null)) return;

            List<String> placas = new ArrayList<>();
            int totalVeiculos = 0;
            JsonElement dados = resp.get("data");
            if (dados != null && dados.isJsonArray()) {
                for (JsonElement e : dados.getAsJsonArray()) {
                    JsonObject o = e.getAsJsonObject();
                    String placa = JsonUtil.texto(o, "licensePlate");
                    String tipoVeiculo = JsonUtil.texto(o, "type");
                    if (placa == null) continue;
                    totalVeiculos++;
                    if (tipoVaga.equals(tipoVeiculo)) placas.add(placa); // só veículos compatíveis
                }
            }

            if (totalVeiculos == 0) {
                Forms.aviso(this, "Você ainda não possui veículos. Cadastre um em \"Meus Veículos\".");
                return;
            }
            if (placas.isEmpty()) {
                Forms.aviso(this, "Você não possui veículo do tipo \"" + tipoVaga
                        + "\" para estacionar na vaga " + vaga + ".");
                return;
            }

            JComboBox<String> cmbPlaca = new JComboBox<>(placas.toArray(new String[0]));
            if (!Forms.mostrar(this, "Estacionar na vaga " + vaga + " (" + tipoVaga + ")",
                    new String[]{"Veículo (placa):"}, new JComponent[]{cmbPlaca})) return;

            final JsonObject campos = new JsonObject();
            campos.addProperty("spotName", vaga);
            campos.addProperty("status", "ocupada");
            campos.addProperty("licensePlate", (String) cmbPlaca.getSelectedItem());
            Async.executar(this, () -> controller.atualizarVaga(campos), r -> {
                if (ok(r, "Veículo estacionado com sucesso!")) carregar();
            });
        });
    }

    /** licensePlate:null desvincula o veículo e deixa a vaga "disponivel". */
    private void liberar() {
        int l = selecionada();
        if (l < 0) return;
        final String vaga = celula(l, COL_VAGA);

        final JsonObject campos = new JsonObject();
        campos.addProperty("spotName", vaga);
        campos.add("licensePlate", JsonNull.INSTANCE);
        Async.executar(this, () -> controller.atualizarVaga(campos), resp -> {
            if (ok(resp, "Vaga liberada com sucesso!")) carregar();
        });
    }
}
