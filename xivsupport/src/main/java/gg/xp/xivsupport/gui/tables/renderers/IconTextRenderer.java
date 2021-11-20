package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.HasIconURL;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class IconTextRenderer {

	private static final Map<Object, Image> cache = new HashMap<>();

	private IconTextRenderer() {
	}

	public static Component getComponent(HasIconURL value, Component defaultLabel) {

		Image scaled = cache.computeIfAbsent(value, job -> {
			URL imageUrl = value.getIcon();
			if (imageUrl == null) {
				return null;
			}
			ImageIcon icon = new ImageIcon(imageUrl);
			return icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
		});
		if (scaled == null) {
			return defaultLabel;
		}
		ImageIcon scaledIcon = new ImageIcon(scaled);
		JLabel label = new JLabel(scaledIcon);
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 0;
		c.ipady = 0;
		c.weightx = 0;
		panel.setOpaque(true);
		panel.setBackground(defaultLabel.getBackground());
		panel.add(label, c);
		c.ipadx = 5;
		c.weightx = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panel.add(defaultLabel, c);
		return panel;

	}
}
