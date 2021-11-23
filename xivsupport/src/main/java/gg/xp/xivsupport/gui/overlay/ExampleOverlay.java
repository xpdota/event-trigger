package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class ExampleOverlay extends XivOverlay {

	public ExampleOverlay(PersistenceProvider persistence) {
		super("Example Overlay", "example-overlay", persistence);
		JPanel panel = new JPanel();
		panel.add(new JLabel("Foo Bar Label Here"));
		JButton button = new JButton("Button");
		panel.add(button);
		panel.setBackground(new Color(200, 100, 0, 255));
		getPanel().add(panel);
	}
}
