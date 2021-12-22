package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;

//@ScanMe
public class ExampleOverlay3 extends XivOverlay {

	public ExampleOverlay3(PersistenceProvider persistence) {
		super("Example Overlay", "example-overlay3", persistence);
		JPanel panel = new JPanel();
		JButton button = new JButton("Bar");
		button.addActionListener(l -> this.dummyMethodForBreakpoint());
		panel.add(button);
		panel.add(new JLabel("Foo Bar Label Here"));
		panel.setBackground(new Color(100, 100, 200, 255));
		getPanel().add(panel);
	}

	private void dummyMethodForBreakpoint() {
		int foo = 5 + 1;
	}
}
