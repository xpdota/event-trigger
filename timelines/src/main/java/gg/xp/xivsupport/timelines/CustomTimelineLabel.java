package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = CustomTimelineLabel.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomTimelineLabel implements CustomTimelineItem {

	@JsonProperty("time")
	public double time;
	@JsonProperty("name")
	public @Nullable String name;
	@JsonProperty("importSource")
	public @Nullable String importSource;

	public static CustomTimelineLabel overrideFor(TimelineEntry item) {
		if (!item.isLabel()) {
			throw new IllegalArgumentException("Cannot override a real entry with a label");
		}
		CustomTimelineLabel out = new CustomTimelineLabel();
		out.time = item.time();
		out.name = item.name();
		return out;
	}

	@JsonProperty("label")
	@Override
	public boolean isLabel() {
		return true;
	}

	@Override
	public @Nullable EventSyncController eventSyncController() {
		return null;
	}

	@Override
	public double time() {
		return time;
	}

	@Override
	public @Nullable String name() {
		return name;
	}

	@Override
	public @Nullable Pattern sync() {
		return null;
	}

	@Override
	public @Nullable Double duration() {
		return null;
	}

	@Override
	public @NotNull TimelineWindow timelineWindow() {
		return TimelineWindow.NONE;
	}

	@Override
	public @Nullable Double jump() {
		return null;
	}

	@Override
	public @Nullable String jumpLabel() {
		return null;
	}

	@Override
	public boolean enabled() {
		return true;
	}

	@Override
	public boolean callout() {
		return false;
	}

	@Override
	public double calloutPreTime() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomTimelineLabel that = (CustomTimelineLabel) o;
		return Double.compare(that.time, time) == 0 && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, name);
	}

	public static CustomTimelineLabel cloneFor(TimelineEntry other) {
		CustomTimelineLabel out = new CustomTimelineLabel();
		out.name = other.name() == null ? "New Label" : (other.name() + " copy");
		out.time = other.time();
		return out;
	}

	@Override
	public void setImportSource(@Nullable String importSource) {
		this.importSource = importSource;
	}

	@Nullable
	@Override
	public String getImportSource() {
		return importSource;
	}
}
