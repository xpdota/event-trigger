package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.platform.unix.X11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

public final class ScalableJFrameLinuxRealImpl extends ScalableJFrame {

	private static final Logger log = LoggerFactory.getLogger(ScalableJFrameLinuxRealImpl.class);
	private static final boolean enableClickThrough;

	static {
		boolean enable;
		try {
			LibUtil.loadLibraryFromJar("/libjawt.so");
			enable = true;
		}
		catch (Throwable t) {
			log.error("Could not load libjawt.so - overlay click-through will not work.");
			enable = false;
		}
		enableClickThrough = enable;
	}

	private double scaleFactor;

	private ScalableJFrameLinuxRealImpl(String title, double scaleFactor) throws HeadlessException {
		super(title);
		this.scaleFactor = scaleFactor;
		JPanel contentPane = new JPanel() {
			private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

			@Override
			public void paint(Graphics g) {
				((Graphics2D) g).setBackground(TRANSPARENT);
				g.clearRect(0, 0, getWidth(), getHeight());
				super.paint(g);
			}

			@Override
			public void paintComponent(Graphics g) {
				((Graphics2D) g).setBackground(TRANSPARENT);
				g.clearRect(0, 0, getWidth(), getHeight());
				super.paintComponent(g);
			}

			@Override
			public void paintChildren(Graphics gg) {
				Graphics2D g = (Graphics2D) gg;
				AffineTransform t = g.getTransform();
				t.scale(ScalableJFrameLinuxRealImpl.this.scaleFactor, ScalableJFrameLinuxRealImpl.this.scaleFactor);
				g.setTransform(t);
				g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
				g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
				super.paintChildren(g);
			}
		};
		contentPane.setOpaque(false);
		contentPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		setContentPane(contentPane);
	}

	public static ScalableJFrame construct(String title, double defaultScaleFactor) {
		return new ScalableJFrameLinuxRealImpl(title, defaultScaleFactor);
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if (getBufferStrategy() == null) {
			createBufferStrategy(2);
		}
	}

	@Override
	public void paint(Graphics g) {
		BufferStrategy buff = getBufferStrategy();
		Graphics drawGraphics = buff.getDrawGraphics();
		Graphics2D g2d = ((Graphics2D) drawGraphics);
		g2d.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
//		g2d.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);

		getContentPane().paint(g2d);
		buff.show();
		g2d.dispose();
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

	/*
		Investigation: Normal "Shape" API isn't the right thing since we still want the window to occupy
		the entire area.

		What we specifically need seems to be XFixesSetWindowShapeRegion(), with a ShapeInput.
		C++ example: https://gist.github.com/ericek111/774a1661be69387de846f5f5a5977a46#file-xoverlay-cpp-L64
		void allow_input_passthrough (Window w) {
		XserverRegion region = XFixesCreateRegion (g_display, NULL, 0);

		//XFixesSetWindowShapeRegion (g_display, w, ShapeBounding, 0, 0, 0);
		XFixesSetWindowShapeRegion (g_display, w, ShapeInput, 0, 0, region);

		XFixesDestroyRegion (g_display, region);
}
	 */

	@Override
	public void setClickThrough(boolean clickThrough) {
		if (!enableClickThrough) {
			return;
		}
		if (clickThrough) {
			Shape shape = new Rectangle(1, 1);
			EnhancedWindowUtils.setWindowMask(this, shape, X11.Xext.ShapeInput);
		}
		else {
			EnhancedWindowUtils.setWindowMask(this, null, X11.Xext.ShapeInput);
		}
	}


}
