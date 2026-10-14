package client.view;

import javax.swing.*;
import java.awt.*;

public class AdminMenuPanel extends JPanel {

    public AdminMenuPanel(Runnable onUsuariosLogados, Runnable onVoltar) {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitulo = new JLabel("Painel de Administração", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(lblTitulo, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(4, 1, 10, 10));

        JButton btnUsuariosLogados = new JButton("1. Gerenciamento de Usuários (Status Online/Offline)");
        JButton btnLogsSistema = new JButton("2. Visualizar Logs do Sistema (Em breve)");
        JButton btnRelatorios = new JButton("3. Relatórios Gerais (Em breve)");
        JButton btnConfiguracoes = new JButton("4. Configurações do Servidor (Em breve)");


        btnUsuariosLogados.addActionListener(e -> onUsuariosLogados.run());

        btnLogsSistema.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Funcionalidade de Logs em desenvolvimento.", "Painel Admin", JOptionPane.INFORMATION_MESSAGE));
        btnRelatorios.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Funcionalidade de Relatórios em desenvolvimento.", "Painel Admin", JOptionPane.INFORMATION_MESSAGE));
        btnConfiguracoes.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Funcionalidade de Configurações em desenvolvimento.", "Painel Admin", JOptionPane.INFORMATION_MESSAGE));

        gridPanel.add(btnUsuariosLogados);
        gridPanel.add(btnLogsSistema);
        gridPanel.add(btnRelatorios);
        gridPanel.add(btnConfiguracoes);

        add(gridPanel, BorderLayout.CENTER);

        JButton btnVoltar = new JButton("Voltar ao Menu Principal");
        btnVoltar.addActionListener(e -> onVoltar.run());

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.add(btnVoltar);
        add(southPanel, BorderLayout.SOUTH);
    }
}