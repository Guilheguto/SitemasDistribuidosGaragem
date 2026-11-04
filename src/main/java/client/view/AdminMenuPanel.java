package client.view;

import javax.swing.*;
import java.awt.*;

public class AdminMenuPanel extends JPanel {

    public AdminMenuPanel(Runnable onUsuarios, Runnable onVagas, Runnable onLocalizarVeiculo,
                          Runnable onStatusUsuarios, Runnable onVoltar) {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel lblTitulo = new JLabel("Painel de Administração", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(lblTitulo, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 1, 10, 10));
        JButton btnUsuarios = new JButton("1. Gerenciar Usuários (consultar, alterar nome, excluir, cadastrar)");
        JButton btnVagas = new JButton("2. Gerenciar Vagas (criar, listar, alterar, excluir)");
        JButton btnLocalizar = new JButton("3. Localizar Veículo pela Placa");
        JButton btnStatus = new JButton("4. Status Online/Offline dos Usuários (extra, só no nosso servidor)");

        btnUsuarios.addActionListener(e -> onUsuarios.run());
        btnVagas.addActionListener(e -> onVagas.run());
        btnLocalizar.addActionListener(e -> onLocalizarVeiculo.run());
        btnStatus.addActionListener(e -> onStatusUsuarios.run());

        grid.add(btnUsuarios);
        grid.add(btnVagas);
        grid.add(btnLocalizar);
        grid.add(btnStatus);
        add(grid, BorderLayout.CENTER);

        JButton btnVoltar = new JButton("Voltar ao Menu Principal");
        btnVoltar.addActionListener(e -> onVoltar.run());
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sul.add(btnVoltar);
        add(sul, BorderLayout.SOUTH);
    }
}
