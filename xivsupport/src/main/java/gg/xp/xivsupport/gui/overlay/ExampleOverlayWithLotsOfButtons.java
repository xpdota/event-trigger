package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;

//@ScanMe
public class ExampleOverlayWithLotsOfButtons extends XivOverlay {

	public ExampleOverlayWithLotsOfButtons(PersistenceProvider persistence, OverlayConfig oc) {
		super("Example Overlay", "example-overlay2", oc, persistence);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(3, 2));
		JButton button1 = new JButton("Bigger");
		button1.addActionListener(l -> {
			setScale(getScale() * 1.1);
		});
		panel.add(button1);
		JButton button2 = new JButton("Smaller");
		button2.addActionListener(l -> {
			setScale(getScale() * 0.9);
		});
		panel.add(button2);
		JButton reset = new JButton("Reset");
		double defaultScale = getScale();
		reset.addActionListener(l -> {
			setScale(defaultScale);
		});

		panel.add(reset);
		JButton nothing = new JButton("Nothing");
		panel.add(nothing);
		panel.setBackground(new Color(200, 100, 0, 255));
		getPanel().add(panel);
		JPanel topPanel = new JPanel();
		topPanel.setBackground(new Color(0, 255, 0, 100));
		getFrame().setGlassPane(topPanel);
		topPanel.setOpaque(false);
		topPanel.setVisible(true);
	}

	private void dummyMethodForBreakpoint() {
		int foo = 5 + 1;
	}
}
