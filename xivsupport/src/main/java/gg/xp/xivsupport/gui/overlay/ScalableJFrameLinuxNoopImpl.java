package gg.xp.xivsupport.gui.overlay;

import java.awt.*;
import java.awt.image.BufferStrategy;

public final class ScalableJFrameLinuxNoopImpl extends ScalableJFrame {


	private ScalableJFrameLinuxNoopImpl(String title) throws HeadlessException {
		super(title);
	}

	public static ScalableJFrame construct(String title) {
		return new ScalableJFrameLinuxNoopImpl(title);
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
		getContentPane().paint(drawGraphics);
//		super.paintComponents(drawGraphics);
		buff.show();
		drawGraphics.dispose();
	}


	public void setScaleFactor(double scaleFactor) {
		pack();
		if (isVisible()) {
			repaint();
		}
	}

	@Override
	public double getScaleFactor() {
		return 1.0;
	}

	@Override
	public void setClickThrough(boolean clickThrough) {

	}
}
