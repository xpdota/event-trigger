package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LaunchImportedSession {
	private static final Logger log = LoggerFactory.getLogger(LaunchImportedSession.class);

	private LaunchImportedSession() {
	}

	public static void fromEvents(List<? extends Event> events) {
		fromEvents(events, false);
	}

	public static void fromEvents(List<? extends Event> events, boolean decompress) {
		// TODO: this needs a fake time source
		CommonGuiSetup.setup();
		MutablePicoContainer pico = XivMain.importInit();
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, events, decompress);
		pico.addComponent(replayController);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.WEBSOCKET_REPLAY);
		dist.acceptEvent(new InitEvent());
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}
}
