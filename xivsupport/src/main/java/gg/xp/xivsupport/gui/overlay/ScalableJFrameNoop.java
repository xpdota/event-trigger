package gg.xp.xivsupport.gui.overlay;

import org.apache.commons.lang3.mutable.MutableDouble;

import java.awt.*;

public class ScalableJFrameNoop extends ScalableJFrame {


	private ScalableJFrameNoop(String title) throws HeadlessException {
		super(title);
	}

	public static ScalableJFrame construct(String title, double defaultScaleFactor, int numBuffers) {
		return new ScalableJFrameNoop(title);
	}

//	@Override
//	public void paint(Graphics g) {
//		super.paint(getGraphics());
//	}
//
//	@Override
//	public void paintComponents(Graphics g) {
//		super.paintComponents(getGraphics());
//	}
//
//	@Override
//	public void paintAll(Graphics g) {
//		super.paintAll(getGraphics());
//	}
//
//	@Override
//	public Graphics getGraphics() {
//		Graphics2D graphics = (Graphics2D) super.getGraphics();
//		AffineTransform transform = graphics.getTransform();
//		transform.scale(scaleFactor.getValue(), scaleFactor.getValue());
////		graphics.setTransform(transform);
//		return graphics;
//	}


	public void setScaleFactor(double scaleFactor) {
//		this.scaleFactor.setValue(scaleFactor);
//		pack();
//		Rectangle bounds = getBounds();
//		int newWidth;
//		int newHeight;
//		// Issues with border when scaling < 1
//		if (scaleFactor < 1.0) {
//			scaleFactor = (5.0 + scaleFactor) / 6.0;
//		}
//		newWidth = (int) Math.round(bounds.width * scaleFactor);
//		newHeight = (int) Math.round(bounds.height * scaleFactor);
//		setBounds(bounds.x, bounds.y, newWidth, newHeight);
//		if (isVisible()) {
//			repaint();
//		}
	}

	@Override
	public double getScaleFactor() {
		return 1.0;
	}
}
