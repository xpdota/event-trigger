package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;

public class ScaledImageComponent extends Component {
	@Serial
	private static final long serialVersionUID = -6148301310440811739L;
	private final Image image;
	private final int size;
	private final Map<Integer, ImageIcon> cache;

	ScaledImageComponent(Image image, int size) {
		this(image, size, new HashMap<>());
	}

	ScaledImageComponent(Image image, int size, Map<Integer, ImageIcon> cache) {
		//noinspection AssignmentOrReturnOfFieldWithMutableType
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

	public int getCurrentSize() {
		return size;
	}

	public ScaledImageComponent cloneThis() {
		return new ScaledImageComponent(image, size, cache);
	}

	public ScaledImageComponent withNewSize(int size) {
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
		ImageIcon scaledImage = cache.computeIfAbsent(scaledSize, newSize -> new ImageIcon(image.getScaledInstance(-1, newSize, Image.SCALE_SMOOTH)));
		scaledImage.paintIcon(this, g, 0, 0);
//			(scaledInstance).paintIcon(this, g, 0, 0);
//			g.drawImage(scaledImage, 0, 0, null);
	}
}
