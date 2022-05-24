package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.OnlineStatus;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.PrimaryPlayerOnlineStatusChangedEvent;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class OverlayMain {

	private static final Logger log = LoggerFactory.getLogger(OverlayMain.class);
	private final BooleanSetting show;
	private final BooleanSetting forceShow;


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
				show.set(false);
				break;
			case "overlay:show":
				show.set(true);
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
				break;
			case "overlay:setallscale":
				if (dbg.getArgs().size() != 2) {
					log.error("Wrong number of arguments, expected 2 ({})", dbg.getArgs());
				}
				setScale(Float.parseFloat(dbg.getArgs().get(1)));
				break;
		}
	}

	@HandleEvents
	public void handlePlayerStatusChanged(EventContext context, PrimaryPlayerOnlineStatusChangedEvent event) {
		this.cutscene = event.getPlayerOnlineStatus() == OnlineStatus.CUTSCENE;
		recalc();
	}

	private boolean windowActive;
	private boolean editing;
	private boolean cutscene;
	// TODO: Linux support
	private final boolean isNonWindows;

	public OverlayMain(PicoContainer container, OverlayConfig config, PersistenceProvider persistence) {
		if (!Platform.isWindows()) {
			log.warn("Not running on Windows - disabling overlay support");
			isNonWindows = true;
		}
		else {
			isNonWindows = false;
		}
		show = config.getShow();
		forceShow = config.getForceShow();
		new Thread(() -> {

			show.addListener(this::recalc);
			forceShow.addListener(this::recalc);

			List<XivOverlay> overlays = container.getComponents(XivOverlay.class);
			overlays.forEach(this::addOverlay);

			setEditing(false);
			//noinspection CallToThreadStartDuringObjectConstruction
			new Thread(() -> {
				while (true) {
					try {
						boolean old = windowActive;
						windowActive = isGameWindowActive();
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
		}, "OverlayStartupHelper").start();
	}

	private boolean isGameWindowActive() {
		if (isNonWindows) {
			return true;
		}
		String window = getActiveWindowText();
		return window.equalsIgnoreCase("FINAL FANTASY XIV") || this.overlays.stream().anyMatch(o -> o.getTitle().equals(window));
	}

	public void addOverlay(XivOverlay overlay) {
		SwingUtilities.invokeLater(() -> {
			overlays.add(overlay);
			overlay.finishInit();
			recalc();
		});
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
		recalc();
	}

	public void setOpacity(float opacity) {
		if (opacity < 0 || opacity > 1) {
			throw new IllegalArgumentException("Opacity must be between 0 and 1, not " + opacity);
		}
		SwingUtilities.invokeLater(() -> overlays.forEach(o -> o.opacity().set(opacity)));
	}

	public void setScale(double scale) {
		if (scale < 0.05 || scale > 10) {
			throw new IllegalArgumentException("Scale must be between 0.05 and 10, not " + scale);
		}
		SwingUtilities.invokeLater(() -> overlays.forEach(o -> o.setScale(scale)));
	}

	private void recalc() {
		// Do this again, otherwise it may flash away when you finish editing
		windowActive = isGameWindowActive();
		// Always show if editing
		// If not editing, show if the user has turned overlay on, and ffxiv is the active window
		boolean shouldShow = editing || (show.get() && (windowActive || forceShow.get()) && !cutscene);
		log.debug("New Overlay State: WindowActive {}; Visible {}; Editing {}", windowActive, shouldShow, editing);
		SwingUtilities.invokeLater(() -> {
			if (shouldShow) {
				overlays.forEach(o -> o.setVisible(true));
				overlays.forEach(o -> o.setEditMode(editing));
			}
			else {
				overlays.forEach(o -> o.setVisible(false));
			}
		});
	}

	private final List<XivOverlay> overlays = new ArrayList<>();

	public List<XivOverlay> getOverlays() {
		return new ArrayList<>(overlays);
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
//		log.info("Window title: [{}]", window);
		return window;
	}
}
