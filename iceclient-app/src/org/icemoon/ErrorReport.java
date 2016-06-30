package org.icemoon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ErrorReport extends JDialog {

    public ErrorReport(Throwable error) {
        super((JDialog) null);
        setLayout(new BorderLayout(8, 8));
        add(new JLabel("Disaster has been axing at the code<br/>"
                + "and caused and exception. Tell her to stop it!"), BorderLayout.NORTH);
        JTextArea text = text = new JTextArea();
        text.setPreferredSize(new Dimension(300, 400));
        add(new JScrollPane(text), BorderLayout.CENTER);
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        JButton report = new JButton("Report");
        JButton close = new JButton("Close");
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        p.add(report);
        p.add(close);
        add(p, BorderLayout.SOUTH);
        pack();
        setVisible(true);
        System.exit(0);
    }
}
