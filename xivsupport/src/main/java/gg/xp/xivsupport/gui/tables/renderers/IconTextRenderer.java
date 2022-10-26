package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.HasIconURL;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IconTextRenderer {

	private static final Map<Object, ScaledImageComponent> cache = new ConcurrentHashMap<>();
	private static final int size = 20;

	private IconTextRenderer() {
	}

	public static Component getComponent(HasIconURL value, Component defaultLabel, boolean iconOnly) {
		return getComponent(value, defaultLabel, iconOnly, false, false, null);
	}

	public static @Nullable AutoHeightScalingIcon getStretchyIcon(HasIconURL value) {
		ScaledImageComponent icon = getIconOnly(value);
		if (icon == null) {
			return null;
		}
		else {
			return new AutoHeightScalingIcon(icon);
		}
	}

	public static @Nullable ScaledImageComponent getIconOnly(HasIconURL value) {
		if (value == null) {
			return null;
		}
		return cache.computeIfAbsent(value, (ignored1) -> {
			URL imageUrl = value.getIconUrl();
			if (imageUrl == null) {
				return null;
			}
			return new ScaledImageComponent(Toolkit.getDefaultToolkit().getImage(imageUrl), size);
		});
	}

	// TODO: we might be able to get rid of the whole 'bypassCache' thing if we use ComponentListRenderer everywhere
	public static Component getComponent(HasIconURL value, Component defaultLabel, boolean iconOnly, boolean textOnleft, boolean bypassCache, @Nullable Component extra) {

		ScaledImageComponent scaled = getIconOnly(value);
		if (scaled != null && bypassCache) {
			scaled = scaled.cloneThis();
		}
		if (scaled == null) {
			return defaultLabel;
		}
		if (iconOnly) {
			return scaled;
		}

		// TODO: this would be faster if we just re-used components
		JPanel panel = new JPanel(false);
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		if (textOnleft) {
			c.weightx = 0;
			if (extra != null) {
				panel.add(extra, c);
			}
			c.ipadx = 0;
			c.ipady = 0;
			c.weightx = 1;
			panel.setOpaque(true);
			panel.setBackground(defaultLabel.getBackground());
			panel.add(defaultLabel, c);
			c.ipadx = 5;
			c.weightx = 0;
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			panel.add(scaled, c);

		}
		else {
			c.ipadx = 0;
			c.ipady = 0;
			c.weightx = 0;
			panel.setOpaque(true);
			panel.setBackground(defaultLabel.getBackground());
			panel.add(scaled, c);
			c.ipadx = 5;
			c.weightx = 1;
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			panel.add(defaultLabel, c);
			c.weightx = 0;
			c.ipadx = 0;
			if (extra != null) {
				panel.add(extra, c);
			}
		}
		return panel;
	}

}
