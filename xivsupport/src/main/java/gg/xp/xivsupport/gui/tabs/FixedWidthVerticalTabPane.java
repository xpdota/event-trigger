package gg.xp.xivsupport.gui.tabs;

import javax.swing.*;
import java.awt.*;

public class FixedWidthVerticalTabPane extends SmartTabbedPane {

	private final int width;

	public FixedWidthVerticalTabPane(int width) {
		super(JTabbedPane.LEFT);
		this.width = width;
	}

	@Override
	public Component add(String title, Component component) {
		Component out = super.add(title, component);
		JLabel label = new JLabel(title);
		label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
		setTabComponentAt(indexOfComponent(out), label);
		return out;
	}

	@Override
	public void addTab(String title, Component component) {
		super.addTab(title, component);
		JLabel label = new JLabel(title);
		label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
		setTabComponentAt(indexOfComponent(component), label);
	}
}
