package gg.xp.xivsupport.gui.imprt;

import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.LaunchImportedActLog;

import java.io.File;
import java.util.List;

public class ACTLogImportSpec implements ImportSpec<ACTLogLineEvent>  {

	private final File file;

	public ACTLogImportSpec(@JsonProperty("file") File file) {
		this.file = file.getAbsoluteFile();
	}

	@JsonProperty("file")
	public File getFile() {
		return file;
	}

	@Override
	public String typeLabel() {
		return "ACT Log";
	}

	@Override
	public String extendedLabel() {
		return file.getName();
	}

	@Override
	public List<ACTLogLineEvent> readEvents() {
		return EventReader.readActLogFile(file);
	}

	@Override
	public void launch(List<ACTLogLineEvent> events) {
		LaunchImportedActLog.fromEvents(events);
	}
}
