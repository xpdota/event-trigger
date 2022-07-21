package gg.xp.xivsupport.gui.overlay;

import org.apache.commons.lang3.mutable.MutableDouble;

import java.awt.*;
import java.awt.geom.AffineTransform;

public final class ScalableJFrameWindowsImpl extends ScalableJFrame {

	private final int numBuffers;
	private final MutableDouble scaleFactor;

	private ScalableJFrameWindowsImpl(String title, MutableDouble scaleFactor, int numBuffers) throws HeadlessException {
		super(title);
		this.scaleFactor = scaleFactor;
		this.numBuffers = numBuffers;
	}

	public static ScalableJFrame construct(String title, double defaultScaleFactor, int numBuffers) {
		MutableDouble scaleFactor = new MutableDouble(defaultScaleFactor);
		return new ScalableJFrameWindowsImpl(title, scaleFactor, numBuffers);
	}

	@Override
	public void setVisible(boolean b) {
		if (getBufferStrategy() == null && numBuffers != 0) {
			createBufferStrategy(numBuffers);
		}
		super.setVisible(b);
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
}
