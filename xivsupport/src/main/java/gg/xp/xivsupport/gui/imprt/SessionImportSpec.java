package gg.xp.xivsupport.gui.imprt;

import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.LaunchImportedSession;

import java.io.File;
import java.util.List;

public class SessionImportSpec implements ImportSpec<Event>  {

	private final File file;
	private final boolean decompress;

	public SessionImportSpec(@JsonProperty("file") File file, @JsonProperty("decompress") boolean decompress) {
		this.file = file.getAbsoluteFile();
		this.decompress = decompress;
	}

	@JsonProperty("file")
	public File getFile() {
		return file;
	}

	@JsonProperty("decompress")
	public boolean getDecompress() {
		return decompress;
	}

	@Override
	public String typeLabel() {
		return "Saved Session";
	}

	@Override
	public String extendedLabel() {
		return file.getName();
	}

	@Override
	public List<Event> readEvents() {
		return EventReader.readEventsFromFile(file);
	}

	@Override
	public void launch(List<Event> events) {
		LaunchImportedSession.fromEvents(events, decompress);
	}
}
