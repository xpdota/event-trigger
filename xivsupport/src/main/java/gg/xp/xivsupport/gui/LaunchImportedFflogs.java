package gg.xp.xivsupport.gui;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.fflogs.FflogsClient;
import gg.xp.xivsupport.events.fflogs.FflogsController;
import gg.xp.xivsupport.events.fflogs.FflogsReportLocator;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LaunchImportedFflogs {
	private static final Logger log = LoggerFactory.getLogger(LaunchImportedFflogs.class);

	private LaunchImportedFflogs() {
	}
	public static void fromEvents(List<? extends Event> events) {
		CommonGuiSetup.setup();
		// TODO: this needs a fake time source
		MutablePicoContainer pico = XivMain.importInit();
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, events, false);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.FFLOGS);
		dist.acceptEvent(new InitEvent());
		pico.addComponent(replayController);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}

	public static void fromUrl(FflogsReportLocator report) {
		CommonGuiSetup.setup();
		// TODO: this needs a fake time source
		MutablePicoContainer pico = XivMain.importInit();

		FflogsController fflogs = pico.addComponent(FflogsController.class).getComponent(FflogsController.class);

		JsonNode jsonNode = fflogs.downloadReport(report);

		List<Event> events = EventReader.readFflogsJson(jsonNode);

		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, events, false);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.FFLOGS);
		dist.acceptEvent(new InitEvent());
		pico.addComponent(replayController);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}
}
