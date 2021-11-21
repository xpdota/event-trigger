package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ScanMe
public class OverlayMain {

	private static final Logger log = LoggerFactory.getLogger(OverlayMain.class);


	@HandleEvents
	public void commands(EventContext context, DebugCommand dbg) {
		String command = dbg.getCommand();
		switch (command) {
			case "overlay:lock":
				setEditing(false);
				break;
			case "overlay:edit":
				setEditing(true);
				break;
			case "overlay:hide":
				setVisible(false);
				break;
			case "overlay:show":
				setVisible(true);
				break;
			case "overlay:windowinfo":
				doWindowInfo();
				break;
			case "overlay:resetall":
				// TODO:
				break;
			case "overlay:setallopacity":
				if (dbg.getArgs().size() != 2) {
					log.error("Wrong number of arguments, expected 2 ({})", dbg.getArgs());
				}
				setOpacity(Float.parseFloat(dbg.getArgs().get(1)));
		}
	}

	private final BooleanSetting show;
	private boolean windowActive;
	private boolean editing;

	public OverlayMain(PersistenceProvider persistence) {
		show = new BooleanSetting(persistence, "xiv-overlay.show", false);
		addOverlay(new ExampleOverlay(persistence));
		setEditing(false);
		//noinspection CallToThreadStartDuringObjectConstruction
		new Thread(() -> {
			while (true) {
				try {
					boolean old = windowActive;
					String window = getActiveWindowText();
					windowActive = window.startsWith("FINAL FANTASY XIV") || overlays.stream().anyMatch(o -> o.getTitle().equals(window));
					if (old != windowActive) {
						recalc();
					}
					//noinspection BusyWait
					Thread.sleep(200);
				}
				catch (Throwable e) {
					log.error("Error", e);
				}
			}
		}).start();
	}

	public void addOverlay(XivOverlay overlay) {
		overlays.add(overlay);
		recalc();
	}

	public void setVisible(boolean visible) {
		show.set(visible);
		recalc();
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
		recalc();
	}

	public void setOpacity(float opacity) {
		if (opacity < 0 || opacity > 1) {
			throw new IllegalArgumentException("Opacity must be between 0 and 1, not " + opacity);
		}
		overlays.forEach(o -> o.setOpacity(opacity));
	}

	private void recalc() {
		// Do this again, otherwise it may flash away when you finish editing
		windowActive = getActiveWindowText().startsWith("FINAL FANTASY XIV");
		// Always show if editing
		// If not editing, show if the user has turned overlay on, and ffxiv is the active window
		boolean shouldShow = editing || (show.get() && windowActive);
		SwingUtilities.invokeLater(() -> {
			if (shouldShow) {
				overlays.forEach(o -> o.setEditMode(editing));
			}
			overlays.forEach(o -> o.setVisible(shouldShow));
		});
	}

	private final List<XivOverlay> overlays = new ArrayList<>();

	private static class ExampleOverlay extends XivOverlay {

		public ExampleOverlay(PersistenceProvider persistence) {
			super("Example Overlay", "example-overlay", persistence);
			JPanel panel = new JPanel();
			panel.add(new JLabel("Foo Bar Label Here"));
			JButton button = new JButton("Button");
			panel.add(button);
			panel.setBackground(new Color(200, 100, 0, 255));
//			panel.setBackground(new Color(255, 128, 0, 128));
			getPanel().add(panel);
//			getFrame().validate();
			getFrame().pack();
		}
	}

	private static void doWindowInfo() {
		log.info("Active window: {}", getActiveWindowText());
	}

	public static String getActiveWindowText() {
		User32 u32 = User32.INSTANCE;
		WinDef.HWND hwnd = u32.GetForegroundWindow();
		int length = u32.GetWindowTextLength(hwnd);
		if (length == 0) return "";
		/* Use the character encoding for the default locale */
		char[] chars = new char[length + 1];
		u32.GetWindowText(hwnd, chars, length + 1);
		String window = new String(chars).substring(0, length);
//		log.info("Window title: {}", window);
		return window;
	}

}
