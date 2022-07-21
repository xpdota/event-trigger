package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;

//@ScanMe
public class ExampleJFrame {

	public static void main(String[] args) {
		CommonGuiSetup.setup();
		JFrame frame = new JFrame("Foo");

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

		frame.setContentPane(panel);
		frame.setUndecorated(true);
		frame.setBackground(new Color(200, 100, 0, 0));
		frame.setSize(new Dimension(500, 500));
		frame.setLocationRelativeTo(null);

		frame.setVisible(true);
	}
}
