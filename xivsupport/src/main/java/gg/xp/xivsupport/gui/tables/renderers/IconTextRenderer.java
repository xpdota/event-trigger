package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.HasIconURL;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;

public final class IconTextRenderer {

	private static final Map<Object, ScaledImageComponent> cache = new HashMap<>();
	private static final int size = 20;

	private IconTextRenderer() {
	}

	public static Component getComponent(HasIconURL value, Component defaultLabel, boolean iconOnly) {
		return getComponent(value, defaultLabel, iconOnly, false, false);
	}

	public static @Nullable Component getIconOnly(HasIconURL value) {
		return getComponent(value, null, true, false, false);
	}

	// TODO: we might be able to get rid of the whole 'bypassCache' thing if we use ComponentListRenderer everywhere
	public static Component getComponent(HasIconURL value, Component defaultLabel, boolean iconOnly, boolean textOnleft, boolean bypassCache) {

		ScaledImageComponent scaled;
		if (bypassCache) {
			ScaledImageComponent existing = cache.get(value);
			if (existing == null) {
				scaled = cache.computeIfAbsent(value, (ignored1) -> {
					URL imageUrl = value.getIcon();
					if (imageUrl == null) {
						return null;
					}
					return new ScaledImageComponent(Toolkit.getDefaultToolkit().getImage(imageUrl), size);
				});
			}
			else {
				scaled = existing.cloneThis();
			}
		}
		else {
			scaled = cache.computeIfAbsent(value, ignored -> {
				URL imageUrl = value.getIcon();
				if (imageUrl == null) {
					return null;
				}
				return new ScaledImageComponent(Toolkit.getDefaultToolkit().getImage(imageUrl), size);
			});
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
		}
		return panel;
	}

	private static class ScaledImageComponent extends JComponent {
		private final Image image;
		private final int size;
		private final Map<Integer, ImageIcon> cache;

		ScaledImageComponent(Image image, int size) {
			this(image, size, new HashMap<>());
		}

		ScaledImageComponent(Image image, int size, Map<Integer, ImageIcon> cache) {
			this.cache = cache;
			this.image = image;
			this.size = size;
			Dimension dims = new Dimension(size, size);
			setMinimumSize(dims);
			setMaximumSize(dims);
			setPreferredSize(dims);
		}

		public ScaledImageComponent cloneThis() {
			return new ScaledImageComponent(image, size, cache);
		}

		@Override
		public void paint(Graphics g) {
			AffineTransform t = ((Graphics2D) g).getTransform();
			((Graphics2D) g).setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
			((Graphics2D) g).setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
			((Graphics2D) g).setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
			double xScale = t.getScaleX();
			double yScale = t.getScaleY();
			t.scale(1 / xScale, 1 / yScale);
			((Graphics2D) g).setTransform(t);
			int scaledSize = (int) (size * yScale);
			// -1 = keep original aspect ratio
			ImageIcon scaledInstance = cache.computeIfAbsent(scaledSize, newSize -> new ImageIcon(image.getScaledInstance(-1, newSize, Image.SCALE_SMOOTH)));
			(scaledInstance).paintIcon(this, g, 0, 0);
		}
	}
}
