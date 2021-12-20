package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.XivMain;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LaunchImportedSession {
	private static final Logger log = LoggerFactory.getLogger(LaunchImportedSession.class);

	private LaunchImportedSession() {
	}

	public static void fromEvents(List<? extends Event> events) {
		CommonGuiSetup.setup();
		MutablePicoContainer pico = XivMain.importInit();
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		PersistenceProvider pers = pico.getComponent(PersistenceProvider.class);
		pers.save("gui.display-predicted-hp", "true");
		ReplayController replayController = new ReplayController(master, events);
		pico.addComponent(replayController);
		dist.acceptEvent(new InitEvent());
		RawEventStorage raw = pico.getComponent(RawEventStorage.class);
		raw.getMaxEventsStoredSetting().set(1_000_000);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
		FailOnThreadViolationRepaintManager.install();
	}
}
