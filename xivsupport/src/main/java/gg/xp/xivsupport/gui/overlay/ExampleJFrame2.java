package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.CommonGuiSetup;

import javax.swing.*;
import java.awt.*;

//@ScanMe
public class ExampleJFrame2 {

	public static void main(String[] args) throws InterruptedException {
		CommonGuiSetup.setup();
		JFrame frame = new JFrame("Foo") {
			{
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

				this.setContentPane(panel);
				this.setUndecorated(true);
				this.setBackground(new Color(200, 100, 0, 0));
				this.setSize(new Dimension(500, 500));
				this.setLocationRelativeTo(null);
			}
		};

		frame.setVisible(true);

		Thread.sleep(5000);
		frame.repaint();
	}
}
