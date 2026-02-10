package gg.xp.xivsupport.gui.imprt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeFflogsTimeSource;
import gg.xp.xivsupport.events.fflogs.FflogsController;
import gg.xp.xivsupport.events.fflogs.FflogsReportLocator;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.GuiMain;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;

import java.util.List;

public class FflogsImportSpec implements ImportSpec<Event> {

	private final String report;
	private final int fight;
	@JsonIgnore
	private MutablePicoContainer pico;

	public FflogsImportSpec(@JsonProperty("report") String report, @JsonProperty("fight") int fight) {
		this.report = report;
		this.fight = fight;
	}

	@JsonProperty("report")
	public String getReport() {
		return report;
	}

	@JsonProperty("fight")
	public int getFight() {
		return fight;
	}

	@Override
	public String typeLabel() {
		return "FFLogs Fight";
	}

	@Override
	public String extendedLabel() {
		int fight = this.fight;
		if (fight == -1) {
			return "Report %s, Last Fight".formatted(report);
		}
		else {
			return "Report %s, Fight #%s".formatted(report, fight);
		}
	}

	@Override
	public EventIterator<Event> eventIter() {
		pico = XivMain.importInit();
		FflogsController fflogs = pico.addComponent(FflogsController.class).getComponent(FflogsController.class);
		List<JsonNode> jsonNode = fflogs.downloadReport(new FflogsReportLocator(report, fight));
		List<Event> events = EventReader.readFflogsJson(jsonNode);
		return new ListEventIterator<>(events);
	}

	@Override
	public void launch(EventIterator<Event> events) {
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		ReplayController replayController = new ReplayController(master, events, false);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.FFLOGS);
		pico.addComponent(FakeFflogsTimeSource.class);
		dist.acceptEvent(new InitEvent());
		pico.addComponent(replayController);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}
}
