package gg.xp.xivsupport.gui.overlay;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

public class OverlayMain {

	private static final Logger log = LoggerFactory.getLogger(OverlayMain.class);

	public static void main(String[] args) throws InterruptedException {
		try {
//			UIManager.setLookAndFeel(new DarculaLaf());
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		}
		catch (Throwable t) {
			log.error("Error setting up look and feel", t);
		}
		TopLevelOverlay topLevel = new TopLevelOverlay("Triggevent Overlay");
		topLevel.start();
		XivOverlay panel = new XivOverlay();
		panel.add(new JLabel("Foo Bar Label Here"));
		panel.add(new JButton("Button"));
		topLevel.add(panel);
//			setTransparent(frame, true);
		while (true) {
			topLevel.setClickThrough(false);
			Thread.sleep(3000);
			topLevel.setClickThrough(true);
			Thread.sleep(3000);
		}
	}

	private static class XivOverlay extends JPanel {
		private int dragX;
		private int dragY;
		public XivOverlay() {
			setOpaque(true);
			setBackground(new Color(255, 128, 0, 128));
			setBorder(new LineBorder(Color.PINK));
			validate();
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent ev) {
					dragX = ev.getXOnScreen();
					dragY = ev.getYOnScreen();
				}
			});
			//....
			//on mouse dragged
			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent evt) {
					int newX = evt.getXOnScreen();
					int newY = evt.getYOnScreen();
					int deltaX = newX - dragX;
					int deltaY = newY - dragY;
					Point old = getLocation();
					setLocation(old.x + deltaX, old.y + deltaY);
					dragX = newX;
					dragY = newY;

				}
			});
			//.....
			//On mouse pressed:
		}
	}

	private static class TopLevelOverlay {

		private final JFrame frame;
		private final JPanel panel;
		private final List<XivOverlay> overlays = new ArrayList<>();

		private static final Color panelBackgroundEditMode = new Color(0, 255, 0, 64);
		private static final Color panelBackgroundDefault = new Color(0, 0, 0, 0);

		public TopLevelOverlay(String title) {

			frame = new JFrame(title);
			frame.setUndecorated(true);
			frame.setBackground(new Color(0, 0, 0, 0));
			frame.setAlwaysOnTop(true);
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			panel = new JPanel();
//			panel.setBorder(new LineBorder(Color.PINK));
			panel.setBackground(panelBackgroundDefault);
			panel.setLayout(null);
			frame.getContentPane().add(panel);

		}

		public void add(XivOverlay overlay) {
			overlays.add(overlay);
			panel.add(overlay);
			Dimension preferredSize = overlay.getPreferredSize();
			overlay.setBounds(200, 200, preferredSize.width, preferredSize.height);
		}

		public void start() {
			frame.setVisible(true);
		}

		public void setClickThrough(boolean clickThrough) {
			OverlayMain.setClickThrough(frame, clickThrough);
			panel.setBackground(clickThrough ? panelBackgroundDefault : panelBackgroundEditMode);
//			panel.repaint();
			frame.repaint();
		}
	}

	private static void setClickThrough(Component w, boolean clickThrough) {
		log.info("Click-through: {}", clickThrough);
		WinDef.HWND hwnd = getHWnd(w);
		int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		log.debug("WL before: {}", wl);
		if (clickThrough) {
			wl |= WinUser.WS_EX_TRANSPARENT;
		}
		else {
			wl &= ~WinUser.WS_EX_TRANSPARENT;
		}
		log.debug("WL after: {}", wl);
		w.setBackground(new Color(0, 0, 0, 0));
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
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
