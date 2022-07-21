package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;

//@ScanMe
public class ExampleJFrame extends JFrame {

	public ExampleJFrame() {
		CommonGuiSetup.setup();
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 2));
		JButton button1 = new JButton("Bigger");
		panel.add(button1);
		JButton button2 = new JButton("Smaller");
		panel.add(button2);
		JButton reset = new JButton("Reset");
		panel.add(reset);
		panel.setOpaque(false);
		panel.setBackground(new Color(200, 100, 0, 0));
		setContentPane(panel);
		setUndecorated(true);
		setBackground(new Color(200, 100, 0, 0));
		setSize(new Dimension(500, 500));
		setLocationRelativeTo(null);
	}

	public static void main(String[] args) {
		new ExampleJFrame().setVisible(true);
	}
}
