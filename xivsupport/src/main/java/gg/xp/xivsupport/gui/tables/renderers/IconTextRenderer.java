package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.jobs.HasIconURL;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.Serial;
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

	public static @Nullable ScaledImageComponent getIconOnly(HasIconURL value) {
		return cache.computeIfAbsent(value, (ignored1) -> {
			URL imageUrl = value.getIcon();
			if (imageUrl == null) {
				return null;
			}
			return new ScaledImageComponent(Toolkit.getDefaultToolkit().getImage(imageUrl), size);
		});
	}

	// TODO: we might be able to get rid of the whole 'bypassCache' thing if we use ComponentListRenderer everywhere
	public static Component getComponent(HasIconURL value, Component defaultLabel, boolean iconOnly, boolean textOnleft, boolean bypassCache) {

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

	private static class ScaledImageComponent extends Component {
		@Serial
		private static final long serialVersionUID = -6148301310440811739L;
		private final Image image;
		private final int size;
		private final Map<Integer, Image> cache;

		ScaledImageComponent(Image image, int size) {
			this(image, size, new HashMap<>());
		}

		ScaledImageComponent(Image image, int size, Map<Integer, Image> cache) {
			this.cache = cache;
			this.image = image;
			this.size = size;
//			int width = image.getWidth(null);
//			int height = image.getHeight(null);
//			Dimension dims = new Dimension((int) Math.ceil((width / (double) height) * size), size);
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
			Image scaledImage = cache.computeIfAbsent(scaledSize, newSize -> image.getScaledInstance(-1, newSize, Image.SCALE_SMOOTH));
			g.drawImage(scaledImage, 0, 0, null);
		}
	}
}
