package gg.xp.xivsupport.gui.util;

import javax.swing.*;
import java.awt.*;

public class GridBagHelper {

	private final JPanel panel;
	private final GridBagConstraints gbc;

	public GridBagHelper(JPanel panel, GridBagConstraints gbc) {
		this.panel = panel;
		if (!(panel.getLayout() instanceof GridBagLayout)) {
			panel.setLayout(new GridBagLayout());
		}
		this.gbc = gbc;
	}

	public void addRow(Component... components) {
		gbc.gridx = 0;
		for (Component component : components) {
			if (component != null) {
				panel.add(component, gbc);
			}
			gbc.gridx++;
		}
		gbc.gridy++;
	}

	public void addVerticalPadding() {
		double oldWeight = gbc.weighty;
		gbc.weighty = 1;
		addRow(Box.createVerticalGlue());
		gbc.weighty = oldWeight;
	}
}
