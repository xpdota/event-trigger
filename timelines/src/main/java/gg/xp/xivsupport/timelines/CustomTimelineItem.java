package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CustomTimelineDeserializer.class)
public interface CustomTimelineItem extends TimelineEntry {
}
