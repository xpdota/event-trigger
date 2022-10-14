package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import gg.xp.xivsupport.persistence.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public final class ScalableJFrameWindowsImpl extends ScalableJFrame {

	private static final Logger log = LoggerFactory.getLogger(ScalableJFrameWindowsImpl.class);

	private final int numBuffers;
	private double scaleFactor;

	private ScalableJFrameWindowsImpl(String title, double scaleFactor, int numBuffers) throws HeadlessException {
		super(title);
		this.scaleFactor = scaleFactor;
		this.numBuffers = numBuffers;
		JPanel contentPane = new JPanel();
		contentPane.setOpaque(false);
		contentPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		setContentPane(contentPane);
	}

	public static ScalableJFrame construct(String title, double defaultScaleFactor, int numBuffers) {
		return new ScalableJFrameWindowsImpl(title, defaultScaleFactor, numBuffers);
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
		transform.scale(scaleFactor, scaleFactor);
		graphics.setTransform(transform);
		return graphics;
	}


	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
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
		return scaleFactor;
	}

	@Override
	public void setClickThrough(boolean clickThrough) {
		setClickThrough(this, clickThrough);
	}

	// https://learn.microsoft.com/en-us/windows/win32/winmsg/window-styles
	// https://docs.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles
	// Other things that may be of note:
	// https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowrgn
	// For performance, WS_EX_TRANSPARENT says that it waits for the underlying window, which I wonder - is this the
	// reason for the small FPS drop when rendering overlays?
	// Also try turning OFF some of the default flags (like WS_CLIPSIBLINGS)
	private static final long WS_EX_NOACTIVATE = 0x08000000L;

	private static void setClickThrough(JFrame w, boolean clickThrough) {
		log.trace("Click-through: {}", clickThrough);
		w.setFocusableWindowState(!clickThrough);
		if (!Platform.isWindows()) {
			log.warn("Setting click-through is not supported on non-Windows platforms at this time.");
			return;
		}
		WinDef.HWND hwnd = getHWnd(w);
		int st = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_STYLE);
		int ex = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		// TODO: test using WS_DISABLED
		if (clickThrough) {
			ex |= WinUser.WS_EX_TRANSPARENT;
			ex |= WS_EX_NOACTIVATE;
//			ex |= WinUser.WS_EX_LAYERED;
//			st |= WinUser.WS_EX_LAYERED;
//			st |= WinUser.WS_DISABLED;
		}
		else {
			ex &= ~WinUser.WS_EX_TRANSPARENT;
			ex &= ~WS_EX_NOACTIVATE;
//			ex &= ~WinUser.WS_EX_LAYERED;
//			st &= ~WinUser.WS_EX_LAYERED;
//			st &= ~WinUser.WS_DISABLED;
		}
		w.setBackground(new Color(0, 0, 0, 0));
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_STYLE, st);
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, ex);
//		if (clickThrough) {
//			User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0, (byte) 100, 2);
//		}
	}

	/**
	 * Get the window handle from the OS
	 */
	private static WinDef.HWND getHWnd(Component w) {
		WinDef.HWND hwnd = new WinDef.HWND();
		hwnd.setPointer(Native.getComponentPointer(w));
		return hwnd;
	}
}
