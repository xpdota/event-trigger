package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class ExampleOverlay extends XivOverlay {

	public ExampleOverlay(PersistenceProvider persistence) {
		super("Example Overlay", "example-overlay2", persistence);
		JPanel panel = new JPanel();
		JButton button = new JButton("Button");
		button.addActionListener(l -> this.dummyMethodForBreakpoint());
		panel.add(button);
		panel.add(new JLabel("Foo Bar Label Here"));
		panel.setBackground(new Color(200, 100, 0, 255));
		getPanel().add(panel);
	}

	private void dummyMethodForBreakpoint() {
		int foo = 5 + 1;
	}
}
