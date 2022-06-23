package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record TimelineWindow(@JsonProperty double start, @JsonProperty double end) implements Serializable {
	public static final TimelineWindow DEFAULT = new TimelineWindow(2.5d, 2.5d);
}
