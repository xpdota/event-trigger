package gg.xp.xivsupport.gui;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeFflogsTimeSource;
import gg.xp.xivsupport.events.fflogs.FflogsController;
import gg.xp.xivsupport.events.fflogs.FflogsFight;
import gg.xp.xivsupport.events.fflogs.FflogsReportLocator;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.imprt.ListEventIterator;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
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
		pico.addComponent(FakeFflogsTimeSource.class);
		ReplayController replayController = new ReplayController(master, new ListEventIterator<>(events), false);
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

		if (!report.fightSpecified()) {

			FflogsReportLocator finalReport = report;
			DecimalFormat percentFormat = new DecimalFormat("0.#%");
			TableWithFilterAndDetails<FflogsFight, Object> table = TableWithFilterAndDetails.builder("Choose a Fight",
							() -> fflogs.getFights(finalReport.report()))
					.addMainColumn(new CustomColumn<>("Fight #", FflogsFight::id, 50))
					.addMainColumn(new CustomColumn<>("Zone", f -> f.zone().getName()))
					.addMainColumn(new CustomColumn<>("Kill/Wipe", f -> f.kill() ? "Kill" : "Wipe", 100))
					.addMainColumn(new CustomColumn<>("Percent", f -> f.kill() ? "" : percentFormat.format(f.fightPercentage()), 100))
					.addMainColumn(new CustomColumn<>("Duration", f -> DurationFormatUtils.formatDuration(f.duration().toMillis(), "m:ss"), 100))
					.build();

			FflogsFight item = ChooserDialog.chooserReturnItem(null, table);
			if (item == null) {
				System.exit(0);
			}
			report = report.withFight(item.id());
		}

		List<JsonNode> jsonNode = fflogs.downloadReport(report);

		List<Event> events = EventReader.readFflogsJson(jsonNode);

		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, new ListEventIterator<>(events), false);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.FFLOGS);
		pico.addComponent(FakeFflogsTimeSource.class);
		dist.acceptEvent(new InitEvent());
		pico.addComponent(replayController);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}
}
