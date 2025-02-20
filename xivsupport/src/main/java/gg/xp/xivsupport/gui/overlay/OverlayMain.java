package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import gg.xp.compmonitor.CompMonitor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.OnlineStatus;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.PrimaryPlayerOnlineStatusChangedEvent;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OverlayMain {

	private static final Logger log = LoggerFactory.getLogger(OverlayMain.class);
	private final BooleanSetting show;
	private final BooleanSetting forceShow;
	private final EventMaster master;
	private final CompMonitor cm;

	@HandleEvents
	public void commands(DebugCommand dbg) {
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
	public void handlePlayerStatusChanged(PrimaryPlayerOnlineStatusChangedEvent event) {
		OnlineStatus status = event.getPlayerOnlineStatus();
		this.cutscene = status == OnlineStatus.CUTSCENE || status == OnlineStatus.GPOSE;
		recalc();
	}

	// CoWAList so that we don't have to worry about weird instantiation race conditions
	private final List<XivOverlay> overlays = new CopyOnWriteArrayList<>();
	private boolean windowActive;
	private boolean editing;
	private boolean cutscene;
	private boolean inCombat;
	private final boolean isWindows;

	public OverlayMain(OverlayConfig config, EventMaster master, CompMonitor cm) {
		this.master = master;
		this.cm = cm;
		isWindows = Platform.isWindows();
		show = config.getShow();
		forceShow = config.getForceShow();
	}

	@HandleEvents
	public void init(InitEvent init) {
		new Thread(() -> {

			show.addListener(this::recalc);
			forceShow.addListener(this::recalc);


			cm.addAndRunListener(ic -> {
				var comp = ic.instance();
				if (comp instanceof XivOverlay overlay) {
					this.addOverlay(overlay);
				}
			});

			setEditing(false);
			new RefreshLoop<>("OverlayStateCheck", this, om -> {
				boolean old = windowActive;
				windowActive = isGameWindowActive();
				if (old != windowActive) {
					recalc();
				}
			}, om -> 200L).start();
			try {
				// Wait for EDT queue to drain
				SwingUtilities.invokeAndWait(() -> {});
			}
			catch (InterruptedException | InvocationTargetException e) {
				//
			}
			master.pushEvent(new OverlaysInitEvent());
		}, "OverlayStartupHelper").start();
	}

	@HandleEvents
	public void inCombatChange(InCombatChangeEvent event) {
		this.inCombat = event.isInCombat();
		recalc();
	}

	private boolean isGameWindowActive() {
		if (!isWindows) {
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
		boolean cutScene = cutscene;
		boolean shouldShow = editing || (show.get() && (windowActive || forceShow.get()) && !cutScene);
		boolean inCombat = this.inCombat;
		log.debug("New Overlay State: WindowActive {}; Visible {}; Editing {}; inCombat {}; cutScene {}", windowActive, shouldShow, editing, inCombat, cutScene);
		SwingUtilities.invokeLater(() -> {
			if (shouldShow) {
				overlays.forEach(o -> {
					if (!inCombat && o.getHideInCombatSetting().get()) {
						o.setVisible(false);
					}
					else {
						o.setVisible(true);
					}
				});
				overlays.forEach(o -> o.setEditMode(editing));
			}
			else {
				overlays.forEach(o -> o.setVisible(false));
			}
		});
	}

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
