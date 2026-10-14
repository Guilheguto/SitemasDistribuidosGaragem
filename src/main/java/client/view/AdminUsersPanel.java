package client.view;

import client.controller.ClientController;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminUsersPanel extends JPanel {

    private static final int INTERVALO_MS = 3000;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ClientController controller;
    private final Runnable onVoltarMenuAdmin;
    private final Runnable onSessaoExpirada;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel lblResumo = new JLabel("Online: 0 | Offline: 0 | Total: 0");
    private final JLabel lblAtualizacao = new JLabel(" ");
    private final Timer refreshTimer;

    private boolean monitorando = false;
    private boolean carregando = false;

    public AdminUsersPanel(ClientController controller, Runnable onVoltarMenuAdmin, Runnable onSessaoExpirada) {
        this.controller = controller;
        this.onVoltarMenuAdmin = onVoltarMenuAdmin;
        this.onSessaoExpirada = onSessaoExpirada;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitulo = new JLabel("Status dos Usuários", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));

        lblResumo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel norte = new JPanel(new GridLayout(2, 1, 0, 4));
        norte.add(lblTitulo);
        norte.add(lblResumo);
        lblResumo.setHorizontalAlignment(SwingConstants.CENTER);
        add(norte, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Usuário", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnAtualizar = new JButton("Atualizar agora");
        btnAtualizar.addActionListener(e -> carregarStatusUsuarios());

        JButton btnVoltar = new JButton("Voltar ao Painel Admin");
        btnVoltar.addActionListener(e -> {
            pararMonitoramento();
            onVoltarMenuAdmin.run();
        });

        lblAtualizacao.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblAtualizacao.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botoes.add(btnAtualizar);
        botoes.add(btnVoltar);

        JPanel sul = new JPanel(new GridLayout(2, 1, 0, 6));
        sul.add(lblAtualizacao);
        sul.add(botoes);
        add(sul, BorderLayout.SOUTH);

        refreshTimer = new Timer(INTERVALO_MS, e -> carregarStatusUsuarios());
    }

    public void iniciarMonitoramento() {
        monitorando = true;
        tableModel.setRowCount(0);
        carregarStatusUsuarios();
        if (!refreshTimer.isRunning()) {
            refreshTimer.start();
        }
    }

    public void pararMonitoramento() {
        monitorando = false;
        refreshTimer.stop();
    }

    private void carregarStatusUsuarios() {
        if (carregando || !monitorando) return;
        carregando = true;

        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                return controller.obterStatusUsuarios();
            }

            @Override
            protected void done() {
                carregando = false;
                if (!monitorando) return;
                try {
                    tratarResposta(get());
                } catch (Exception e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    lblAtualizacao.setText("Falha de comunicação: " + causa.getMessage());
                }
            }
        }.execute();
    }

    private void tratarResposta(JsonObject resp) {
        int status = lerStatusCode(resp);

        if (status == 200) {
            atualizarTabela(extrairUsuarios(resp));
            lblAtualizacao.setText("Última atualização: " + LocalTime.now().format(HORA));
            return;
        }

        if (status == 401) {
            pararMonitoramento();
            JOptionPane.showMessageDialog(this, "Sessão inválida ou expirada. Faça login novamente.",
                    "Acesso Não Autorizado (401)", JOptionPane.ERROR_MESSAGE);
            onSessaoExpirada.run();
        } else if (status == 403) {
            pararMonitoramento();
            JOptionPane.showMessageDialog(this, "Acesso restrito ao administrador.",
                    "Acesso Negado (403)", JOptionPane.WARNING_MESSAGE);
            onVoltarMenuAdmin.run();
        } else {
            lblAtualizacao.setText("Erro do servidor (código " + status + ")");
        }
    }

    private JsonArray extrairUsuarios(JsonObject resp) {
        JsonElement data = resp.get("data");
        if (data != null && data.isJsonObject()) {
            JsonElement users = data.getAsJsonObject().get("users");
            if (users != null && users.isJsonArray()) {
                return users.getAsJsonArray();
            }
        }
        return new JsonArray();
    }

    private void atualizarTabela(JsonArray usuarios) {
        int selecionada = table.getSelectedRow();
        Object selecionadoNome = selecionada >= 0 ? table.getValueAt(selecionada, 0) : null;

        tableModel.setRowCount(0);
        int online = 0;

        for (JsonElement elem : usuarios) {
            JsonObject obj = elem.getAsJsonObject();
            String user = obj.get("username").getAsString();
            String st = obj.get("status").getAsString();
            if ("ONLINE".equalsIgnoreCase(st)) online++;
            tableModel.addRow(new Object[]{user, st});
        }

        int total = usuarios.size();
        lblResumo.setText("Online: " + online + " | Offline: " + (total - online) + " | Total: " + total);

        if (selecionadoNome != null) {
            for (int i = 0; i < table.getRowCount(); i++) {
                if (selecionadoNome.equals(table.getValueAt(i, 0))) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private int lerStatusCode(JsonObject resp) {
        if (resp == null) return -1;
        JsonElement el = resp.get("statusCode");
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) return -1;
        return el.getAsInt();
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, value, sel, focus, r, c);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!sel) {
                boolean on = "ONLINE".equalsIgnoreCase(String.valueOf(value));
                comp.setForeground(on ? new Color(0, 130, 0) : Color.GRAY);
            }
            return comp;
        }
    }
}