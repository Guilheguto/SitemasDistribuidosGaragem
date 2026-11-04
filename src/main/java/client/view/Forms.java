package client.view;

import javax.swing.*;
import java.awt.Component;
import java.awt.GridLayout;

final class Forms {
    private Forms() {}

    /** Mostra um formulário simples (rótulo + campo por linha). Devolve true se o usuário confirmou. */
    static boolean mostrar(Component parent, String titulo, String[] rotulos, JComponent[] campos) {
        JPanel painel = new JPanel(new GridLayout(rotulos.length, 2, 8, 8));
        for (int i = 0; i < rotulos.length; i++) {
            painel.add(new JLabel(rotulos[i]));
            painel.add(campos[i]);
        }
        return JOptionPane.showConfirmDialog(parent, painel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    static void aviso(Component parent, String mensagem) {
        JOptionPane.showMessageDialog(parent, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    /** Converte para inteiro; devolve null se não for um número inteiro válido. */
    static Integer inteiro(String texto) {
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
