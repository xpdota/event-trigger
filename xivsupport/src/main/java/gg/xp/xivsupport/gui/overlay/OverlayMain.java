package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.OnlineStatus;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.PrimaryPlayerOnlineStatusChangedEvent;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class OverlayMain {

	private static final Logger log = LoggerFactory.getLogger(OverlayMain.class);
	private final BooleanSetting show;
	private final BooleanSetting forceShow;
	private final PicoContainer container;
	private final EventMaster master;


	@HandleEvents
	public void handlePlayerStatusChanged(EventContext context, PrimaryPlayerOnlineStatusChangedEvent event) {
		OnlineStatus status = event.getPlayerOnlineStatus();
		this.cutscene = status == OnlineStatus.CUTSCENE || status == OnlineStatus.GPOSE;
		recalc();
	}

	private boolean windowActive;
	private boolean editing;
	private boolean cutscene;
	// TODO: Linux support
	private final ActiveWindowTitleGetter winUtil;

	public OverlayMain(MutablePicoContainer container, OverlayConfig config, EventMaster master) {
		this.master = master;
		if (!Platform.isWindows()) {
			log.warn("Not running on Windows - disabling overlay support");
			winUtil = new DummyWindowGetter();
		}
		else {
			ActiveWindowTitleGetter winUtil;
			try {
				winUtil = new WindowsUtils();
			}
			catch (Throwable t) {
				log.error("Error initializing WindowsUtils", t);
				winUtil = new DummyWindowGetter();
			}
			container.addComponent(winUtil);
			this.winUtil = winUtil;
		}
		show = config.getShow();
		forceShow = config.getForceShow();
		this.container = container;
	}

	@HandleEvents
	public void init(EventContext context, InitEvent init) {
		new Thread(() -> {

			show.addListener(this::recalc);
			forceShow.addListener(this::recalc);

			List<XivOverlay> overlays = container.getComponents(XivOverlay.class);
			overlays.forEach(this::addOverlay);

			setEditing(false);
			new RefreshLoop<>("OverlayStateCheck", this, om -> {
				boolean old = windowActive;
				windowActive = isGameWindowActive();
				if (old != windowActive) {
					recalc();
				}
			}, om -> 200L).start();
			try {
				SwingUtilities.invokeAndWait(() -> {
				});
			}
			catch (InterruptedException | InvocationTargetException e) {
				//
			}
			master.pushEvent(new OverlaysInitEvent());
		}, "OverlayStartupHelper").start();

	}

	private boolean isGameWindowActive() {
		return winUtil.isFfxivActive();
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

	private void doWindowInfo() {
		log.info("Active window: {}", winUtil.getActiveWindowTitle());
	}


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

	private static class DummyWindowGetter implements ActiveWindowTitleGetter {
		@Override
		public String getActiveWindowTitle() {
			return "Unknown";
		}

		@Override
		public boolean isFfxivActive() {
			return true;
		}
	}
}
