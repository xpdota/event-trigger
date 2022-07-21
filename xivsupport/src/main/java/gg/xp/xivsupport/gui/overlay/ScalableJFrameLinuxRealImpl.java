package gg.xp.xivsupport.gui.overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;

public final class ScalableJFrameLinuxRealImpl extends ScalableJFrame {

	private final int numBuffers;
	private double scaleFactor;

	private ScalableJFrameLinuxRealImpl(String title, double scaleFactor, int numBuffers) throws HeadlessException {
		super(title);
		this.scaleFactor = scaleFactor;
		this.numBuffers = numBuffers;
	}

	public static ScalableJFrame construct(String title, double defaultScaleFactor, int numBuffers) {
		return new ScalableJFrameLinuxRealImpl(title, defaultScaleFactor, numBuffers);
	}

	@Override
	public void setVisible(boolean b) {
		if (getBufferStrategy() == null) {
			createBufferStrategy(2);
		}
		super.setVisible(b);
	}

	@Override
	public void paint(Graphics g) {
		BufferStrategy buff = getBufferStrategy();
		Graphics drawGraphics = buff.getDrawGraphics();
		Graphics2D g2d = ((Graphics2D) drawGraphics);
		AffineTransform t = g2d.getTransform();
		t.scale(scaleFactor, scaleFactor);
		g2d.transform(t);
		g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
//		g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);

		getContentPane().paint(drawGraphics);
//		super.paintComponents(drawGraphics);
		buff.show();
		drawGraphics.dispose();
	}


	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
		Dimension pref = getContentPane().getPreferredSize();
		Rectangle bounds = getBounds();
		int newWidth;
		int newHeight;
		// Issues with border when scaling < 1
		if (scaleFactor < 1.0) {
			scaleFactor = (5.0 + scaleFactor) / 6.0;
		}
		newWidth = (int) Math.round(pref.width * scaleFactor);
		newHeight = (int) Math.round(pref.height * scaleFactor);
//		setVisible(wasVisible);
		SwingUtilities.invokeLater(() -> {
			setBounds(bounds.x, bounds.y, newWidth, newHeight);
			if (isVisible()) {
//				revalidate();
				repaint();
			}
		});
	}

	@Override
	public double getScaleFactor() {
		return scaleFactor;
	}
}
