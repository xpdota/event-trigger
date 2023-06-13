package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.actlines.parsers.FakeTimeSource;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.imprt.EventIterator;
import gg.xp.xivsupport.gui.util.CatchFatalError;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LaunchImportedActLog {
	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private static final Logger log = LoggerFactory.getLogger(LaunchImportedActLog.class);

	private LaunchImportedActLog() {
	}

	public static void fromEvents(EventIterator<? extends Event> events) {
		fromEvents(events, false);
	}


	public static void fromEvents(EventIterator<? extends Event> events, boolean decompress) {
		CommonGuiSetup.setup();
		MutablePicoContainer pico = XivMain.importInit();
		pico.addComponent(FakeACTTimeSource.class);
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, events, decompress);
		pico.addComponent(replayController);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.ACT_LOG_FILE);
		dist.acceptEvent(new InitEvent());
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}
}
