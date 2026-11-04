package client.view;

import javax.swing.*;
import java.awt.*;

/** Mostra as mensagens enviadas e recebidas pelo cliente (exigência da entrega). */
/*public class MessageLogPanel extends JPanel {

    private final JTextArea area = new JTextArea(7, 60);

    public MessageLogPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Mensagens enviadas e recebidas"));

        area.setEditable(false);
        area.setLineWrap(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        add(new JScrollPane(area), BorderLayout.CENTER);

        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.addActionListener(e -> area.setText(""));
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        sul.add(btnLimpar);
        add(sul, BorderLayout.SOUTH);
    }


    public void adicionar(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            area.append(mensagem + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
    }
}
*/