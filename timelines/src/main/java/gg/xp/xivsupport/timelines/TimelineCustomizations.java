package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.persistence.UseJsonSer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@UseJsonSer
public class TimelineCustomizations {
	@JsonProperty
	private List<CustomTimelineEntry> entries = Collections.emptyList();

	public List<CustomTimelineEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public void setEntries(List<CustomTimelineEntry> entries) {
		this.entries = new ArrayList<>(entries);
		this.entries.sort(Comparator.naturalOrder());
	}
}
