package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.persistence.Platform;
import org.apache.commons.lang3.mutable.MutableDouble;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;

public class ScalableJFrame extends JFrame implements Scaled {

	private final MutableDouble scaleFactor;
	private final GraphicsConfiguration fakeGraphics;

	private ScalableJFrame(String title, GraphicsConfiguration gc, MutableDouble scaleFactor) throws HeadlessException {
		super(title, gc);
		this.fakeGraphics = gc;
		this.scaleFactor = scaleFactor;
	}

	public static ScalableJFrame construct(String title, double defaultScaleFactor) {
		MutableDouble scaleFactor = new MutableDouble(defaultScaleFactor);
		// TODO: find a better way for this
		if (Platform.isWindows()) {
			return new ScalableJFrame(title, new FakeGraphicsConfiguration(scaleFactor), scaleFactor);
		}
		else {
			return new ScalableJFrame(title, new JFrame().getGraphicsConfiguration(), scaleFactor);
		}
	}


	@Override
	public GraphicsConfiguration getGraphicsConfiguration() {
		return fakeGraphics;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(getGraphics());
	}

	@Override
	public void paintComponents(Graphics g) {
		super.paintComponents(getGraphics());
	}

	@Override
	public void paintAll(Graphics g) {
		super.paintAll(getGraphics());
	}

	@Override
	public Graphics getGraphics() {
		Graphics2D graphics = (Graphics2D) super.getGraphics();
		AffineTransform transform = graphics.getTransform();
		transform.scale(scaleFactor.getValue(), scaleFactor.getValue());
		graphics.setTransform(transform);
		return graphics;
	}

	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor.setValue(scaleFactor);
		pack();
		Rectangle bounds = getBounds();
		int newWidth;
		int newHeight;
		// Issues with border when scaling < 1
		if (scaleFactor < 1.0) {
			scaleFactor = (5.0 + scaleFactor) / 6.0;
		}
		newWidth = (int) Math.round(bounds.width * scaleFactor);
		newHeight = (int) Math.round(bounds.height * scaleFactor);
		setBounds(bounds.x, bounds.y, newWidth, newHeight);
		if (isVisible()) {
			repaint();
		}
	}

	@Override
	public double getScaleFactor() {
		return scaleFactor.getValue();
	}

	// TODO: find an X11-friendly system for this
	private static final class FakeGraphicsConfiguration extends GraphicsConfiguration {
		private final GraphicsConfiguration wrapped;
		private final MutableDouble scaleFactor;

		FakeGraphicsConfiguration(MutableDouble scaleFactor) {
			this.scaleFactor = scaleFactor;
			this.wrapped = new JFrame().getGraphicsConfiguration();
		}

		@Override
		public GraphicsDevice getDevice() {
			return wrapped.getDevice();
		}

		@Override
		public ColorModel getColorModel() {
			return wrapped.getColorModel();
		}

		@Override
		public ColorModel getColorModel(int transparency) {
			return wrapped.getColorModel(transparency);
		}

		@Override
		public AffineTransform getDefaultTransform() {
			AffineTransform defaultTransform = wrapped.getDefaultTransform();
			defaultTransform.scale(scaleFactor.getValue(), scaleFactor.getValue());
//			log.info("getDefaultTransform");
			return defaultTransform;
		}

		@Override
		public AffineTransform getNormalizingTransform() {
			AffineTransform defaultTransform = wrapped.getDefaultTransform();
			defaultTransform.scale(scaleFactor.getValue(), scaleFactor.getValue());
//			log.info("getNormalizingTransform");
			return defaultTransform;
		}

		@Override
		public Rectangle getBounds() {
//			Rectangle bounds = wrapped.getBounds();
//			log.info("getBounds: {} {} {} {}", bounds.x, bounds.y, bounds.width, bounds.height);
//			return new Rectangle(bounds.x, bounds.y, (int) (bounds.width * scaleFactor), (int) (bounds.height * scaleFactor));
			return wrapped.getBounds();
		}
	}
}
