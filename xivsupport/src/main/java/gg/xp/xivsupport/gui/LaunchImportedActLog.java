package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LaunchImportedActLog {
	private static final Logger log = LoggerFactory.getLogger(LaunchImportedActLog.class);

	private LaunchImportedActLog() {
	}
	public static void fromEvents(List<? extends Event> events) {
		fromEvents(events, false);
	}

	public static void fromEvents(List<? extends Event> events, boolean decompress) {
		CommonGuiSetup.setup();
		MutablePicoContainer pico = XivMain.importInit();
		pico.addComponent(FakeACTTimeSource.class);
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		PersistenceProvider pers = pico.getComponent(PersistenceProvider.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, events, decompress);
		pico.addComponent(replayController);
		dist.acceptEvent(new InitEvent());
		pico.getComponent(XivStateImpl.class).setActImport(true);
		RawEventStorage raw = pico.getComponent(RawEventStorage.class);
		raw.getMaxEventsStoredSetting().set(1_000_000);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
//		FailOnThreadViolationRepaintManager.install();
	}
}
