package gg.xp.xivsupport.timelines;

import java.io.Serializable;

public record TimelineReference(double time, String name, String pattern) implements Serializable {
	public static TimelineReference of(TimelineEntry other) {
		other = other.untranslated();
		return new TimelineReference(other.time(), other.name(), other.sync() == null ? null : other.sync().pattern());
	}
}
