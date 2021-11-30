package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.DoubleSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;

@SuppressWarnings("NumericCastThatLosesPrecision")
public class XivOverlay {
	private static final Logger log = LoggerFactory.getLogger(XivOverlay.class);
	private static final Color panelBackgroundEditMode = new Color(0, 255, 0, 64);
	private static final Color panelBackgroundDefault = new Color(0, 0, 0, 0);
	private static final LineBorder editBorder = new LineBorder(Color.PINK, 5);
	private static final LineBorder transparentBorder = new LineBorder(new Color(0, 0, 0, 0), 5);
	private int dragX;
	private int dragY;
	private final JFrame frame;
	private final JPanel panel;
	private final LongSetting xSetting;
	private final LongSetting ySetting;
	private final DoubleSetting opacity;
	private final DoubleSetting scaleFactor;
	private final String title;

	private final class FakeGraphicsConfiguration extends GraphicsConfiguration {
		private final GraphicsConfiguration wrapped;

		FakeGraphicsConfiguration(GraphicsConfiguration wrapped) {
			this.wrapped = wrapped;
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
			defaultTransform.scale(scaleFactor.get(), scaleFactor.get());
			log.info("getDefaultTransform");
			return defaultTransform;
		}

		@Override
		public AffineTransform getNormalizingTransform() {
			AffineTransform defaultTransform = wrapped.getDefaultTransform();
			defaultTransform.scale(scaleFactor.get(), scaleFactor.get());
			log.info("getNormalizingTransform");
			return defaultTransform;
		}

		@Override
		public Rectangle getBounds() {
//			Rectangle bounds = wrapped.getBounds();
//			log.info("getBounds: {} {} {} {}", bounds.x, bounds.y, bounds.width, bounds.height);
//			return new Rectangle(bounds.x, bounds.y, (int) (bounds.width * scaleFactor.get()), (int) (bounds.height * scaleFactor.get()));
			return wrapped.getBounds();
		}
	}

	public XivOverlay(String title, String settingKeyBase, PersistenceProvider persistence) {
		xSetting = new LongSetting(persistence, String.format("xiv-overlay.window-pos.%s.x", settingKeyBase), 200);
		ySetting = new LongSetting(persistence, String.format("xiv-overlay.window-pos.%s.y", settingKeyBase), 200);
		opacity = new DoubleSetting(persistence, String.format("xiv-overlay.window-pos.%s.opacity", settingKeyBase), 1.0d);
		scaleFactor = new DoubleSetting(persistence, String.format("xiv-overlay.window-pos.%s.scale", settingKeyBase), 2.0d);
		JFrame fakeFrame = new JFrame();
		GraphicsConfiguration gc = fakeFrame.getGraphicsConfiguration();
		FakeGraphicsConfiguration fakeGraphics = new FakeGraphicsConfiguration(gc);
		frame = new JFrame(title, fakeGraphics) {
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
				transform.scale(scaleFactor.get(), scaleFactor.get());
				graphics.setTransform(transform);
				return graphics;
			}

		};
		this.title = title;
		frame.setUndecorated(true);
		frame.setBackground(panelBackgroundDefault);
		frame.setType(Window.Type.UTILITY);
		panel = new JPanel();
		panel.setOpaque(false);
//			panel.add(contents);
		panel.setBorder(transparentBorder);
		frame.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT));
		frame.getContentPane().add(panel);
		frame.setAlwaysOnTop(true);
		frame.setLocation((int) xSetting.get(), (int) ySetting.get());
		frame.setOpacity((float) opacity.get());
		frame.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent ev) {
				dragX = ev.getXOnScreen();
				dragY = ev.getYOnScreen();
			}
		});
		frame.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent evt) {
				int newX = evt.getXOnScreen();
				int newY = evt.getYOnScreen();
				int deltaX = newX - dragX;
				int deltaY = newY - dragY;
				Point old = frame.getLocation();
				setPosition(old.x + deltaX, old.y + deltaY);
				dragX = newX;
				dragY = newY;
			}
		});
	}

	public void finishInit() {
		frame.repaint();
		frame.pack();
		Rectangle bounds = frame.getBounds();
		frame.setBounds(bounds.x, bounds.y, (int) (bounds.width * scaleFactor.get()), (int) (bounds.height * scaleFactor.get()));
		frame.validate();
		frame.repaint();
	}

	public String getTitle() {
		return title;
	}

	protected JFrame getFrame() {
		return frame;
	}

	protected JPanel getPanel() {
		return panel;
	}

	public void setPosition(int x, int y) {
		frame.setLocation(x, y);
		xSetting.set(x);
		ySetting.set(y);
	}

	public void setVisible(boolean visible) {
		if (visible) {
			frame.setVisible(true);
		}
		panel.setVisible(visible);
	}

	public void setEditMode(boolean editMode) {
		setClickThrough(frame, !editMode);
		if (editMode) {
			panel.setBorder(editBorder);
		}
		else {
			panel.setBorder(transparentBorder);
		}
	}

	public void setOpacity(float opacity) {
		this.opacity.set(opacity);
		frame.setOpacity(opacity);
	}

	private static void setClickThrough(Component w, boolean clickThrough) {
		log.trace("Click-through: {}", clickThrough);
		WinDef.HWND hwnd = getHWnd(w);
		int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		if (clickThrough) {
			wl |= WinUser.WS_EX_TRANSPARENT;
		}
		else {
			wl &= ~WinUser.WS_EX_TRANSPARENT;
		}
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
