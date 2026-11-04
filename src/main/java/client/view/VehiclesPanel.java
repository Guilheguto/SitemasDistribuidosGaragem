package client.view;

import client.controller.ClientController;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Veículos do próprio usuário (createvehicle, getvehicles, updatevehicle, deletevehicle). */
public class VehiclesPanel extends JPanel {

    private static final String[] TIPOS = {"eletrico", "combustao"};

    private final ClientController controller;
    private final Runnable onSessaoExpirada;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblInfo = new JLabel(" ");

    public VehiclesPanel(ClientController controller, Runnable onVoltar, Runnable onSessaoExpirada) {
        this.controller = controller;
        this.onSessaoExpirada = onSessaoExpirada;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Meus Veículos", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(titulo, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Placa", "Marca", "Modelo", "Tipo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnAtualizar = new JButton("Atualizar");
        JButton btnNovo = new JButton("Novo Veículo");
        JButton btnEditar = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnVoltar = new JButton("Voltar");
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        botoes.add(btnAtualizar);
        botoes.add(btnNovo);
        botoes.add(btnEditar);
        botoes.add(btnExcluir);
        botoes.add(btnVoltar);

        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel sul = new JPanel(new GridLayout(2, 1, 0, 4));
        sul.add(lblInfo);
        sul.add(botoes);
        add(sul, BorderLayout.SOUTH);

        btnAtualizar.addActionListener(e -> carregar());
        btnNovo.addActionListener(e -> criar());
        btnEditar.addActionListener(e -> editar());
        btnExcluir.addActionListener(e -> excluir());
        btnVoltar.addActionListener(e -> onVoltar.run());
    }

    public void abrir() {
        model.setRowCount(0);
        carregar();
    }

    private boolean ok(JsonObject resp, String msgSucesso) {
        return ResponseHandler.processarResposta(this, resp, msgSucesso, onSessaoExpirada);
    }

    private void carregar() {
        Async.executar(this, controller::listarVeiculos, resp -> {
            if (!ok(resp, null)) return;
            model.setRowCount(0);
            JsonElement dados = resp.get("data");
            if (dados != null && dados.isJsonArray()) {
                for (JsonElement e : dados.getAsJsonArray()) {
                    JsonObject o = e.getAsJsonObject();
                    String modelo = JsonUtil.texto(o, "model");
                    model.addRow(new Object[]{JsonUtil.texto(o, "licensePlate"), JsonUtil.texto(o, "brand"),
                            modelo == null ? "-" : modelo, JsonUtil.texto(o, "type")});
                }
            }
            lblInfo.setText(model.getRowCount() + " veículo(s)");
        });
    }

    private int selecionado() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Forms.aviso(this, "Selecione um veículo na tabela.");
            return -1;
        }
        return table.convertRowIndexToModel(r);
    }

    private String celula(int linha, int coluna) {
        Object v = model.getValueAt(linha, coluna);
        return v == null ? "" : v.toString();
    }

    private void criar() {
        JTextField txtPlaca = new JTextField();
        JTextField txtMarca = new JTextField();
        JTextField txtModelo = new JTextField();
        JComboBox<String> cmbTipo = new JComboBox<>(TIPOS);
        if (!Forms.mostrar(this, "Novo veículo",
                new String[]{"Placa (7 caracteres):", "Marca (até 20):", "Modelo (opcional, até 20):", "Tipo:"},
                new JComponent[]{txtPlaca, txtMarca, txtModelo, cmbTipo})) return;

        final String placa = txtPlaca.getText().trim();
        final String marca = txtMarca.getText().trim();
        final String modelo = txtModelo.getText().trim().isEmpty() ? null : txtModelo.getText().trim();
        final String tipo = (String) cmbTipo.getSelectedItem();

        Async.executar(this, () -> controller.criarVeiculo(placa, marca, modelo, tipo), resp -> {
            if (ok(resp, "Veículo cadastrado com sucesso!")) carregar();
        });
    }

    private void editar() {
        int l = selecionado();
        if (l < 0) return;
        final String placa = celula(l, 0);
        String modeloAtual = celula(l, 2);
        if ("-".equals(modeloAtual)) modeloAtual = "";

        JTextField txtMarca = new JTextField(celula(l, 1));
        JTextField txtModelo = new JTextField(modeloAtual);
        JComboBox<String> cmbTipo = new JComboBox<>(TIPOS);
        cmbTipo.setSelectedItem(celula(l, 3));
        if (!Forms.mostrar(this, "Editar veículo " + placa,
                new String[]{"Marca:", "Modelo (vazio = limpar):", "Tipo:"},
                new JComponent[]{txtMarca, txtModelo, cmbTipo})) return;

        final String marca = txtMarca.getText().trim();
        final String modelo = txtModelo.getText().trim().isEmpty() ? null : txtModelo.getText().trim();
        final String tipo = (String) cmbTipo.getSelectedItem();

        Async.executar(this, () -> controller.atualizarVeiculo(placa, marca, modelo, tipo), resp -> {
            if (ok(resp, "Veículo atualizado com sucesso!")) carregar();
        });
    }

    private void excluir() {
        int l = selecionado();
        if (l < 0) return;
        final String placa = celula(l, 0);
        int opcao = JOptionPane.showConfirmDialog(this, "Excluir o veículo " + placa + "?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcao != JOptionPane.YES_OPTION) return;

        Async.executar(this, () -> controller.removerVeiculo(placa), resp -> {
            if (ok(resp, "Veículo removido com sucesso!")) carregar();
        });
    }
}
