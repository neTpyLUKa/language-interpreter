// Java Program to create a text editor using java

import org.solution.InterpreterLauncher;
import org.solution.Writer;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class editor extends JFrame implements ActionListener {
    // Text component
    JTextArea t;
    JTextArea t_other;
    // Frame
    JFrame f;
    JFrame f_other;
    InterpreterLauncher launcher;

    // Constructor
    editor() {
        f = new JFrame("editor");

        try {
            // Set metl look and feel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

            // Set theme to ocean
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        } catch (Exception ignored) {
        }

        // Text component
        t = new JTextArea();

        f_other = new JFrame("output");
        t_other = new JTextArea();
        f_other.add(t_other);
        f_other.setSize(500, 250);
        Thread thread = new Thread(() -> f_other.setVisible(true));
        thread.start();

        launcher = new InterpreterLauncher(t, t_other, new Writer(t_other));
        // Create a menubar
        JMenuBar mb = new JMenuBar();

        JMenuItem mc = new JMenuItem("Run");

        mc.addActionListener(this);

        mb.add(mc);

        t_other.setText("Hello, World!");
        f.setJMenuBar(mb);
        f.add(t);
        f.setSize(250, 500);
        launcher.buildSyntaxTree();
        f.setVisible(true);
    }

    // If a button is pressed
    public void actionPerformed(ActionEvent e) {
        launcher.runInterpreter();
    }

    // Main class
    public static void main(String[] args) {
        new editor();
    }
}
