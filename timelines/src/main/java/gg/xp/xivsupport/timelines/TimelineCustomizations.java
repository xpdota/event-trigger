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
	public boolean enabled = true;
	@JsonProperty
	private List<CustomTimelineItem> entries = Collections.emptyList();

	public List<CustomTimelineItem> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public void setEntries(List<CustomTimelineItem> entries) {
		this.entries = new ArrayList<>(entries);
		this.entries.sort(Comparator.naturalOrder());
	}
}
