package gg.xp.xivsupport.timelines;

import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CustomTimelineDeserializer.class)
public interface CustomTimelineItem extends TimelineEntry {
	void setImportSource(String importSource);
}
