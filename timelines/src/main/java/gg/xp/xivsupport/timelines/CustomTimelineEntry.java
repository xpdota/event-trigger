package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

// Just going to use jackson for now
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomTimelineEntry implements TimelineEntry, Serializable {

	@Serial
	private static final long serialVersionUID = 8590938155631598982L;
	// TODO: encapsulate these better
	public double time;
	public @Nullable String name;
	public @Nullable Pattern sync;
	public @Nullable Double duration;
	public @Nullable Double windowStart;
	public @Nullable Double windowEnd;
	public @Nullable Double jump;
	// TODO: this uses the absolute path to the JAR, which means icons will break if the user moves their install location.
	// Best solution is to probably make our own little class that lets you specify an ability/status ID in addition to
	// plain URLs.
	public @Nullable URL icon;
	private @Nullable TimelineReference replaces;
	public boolean enabled = true;
	public boolean callout;
	public double calloutPreTime;
	public CombatJobSelection enabledJobs = CombatJobSelection.all();

	public CustomTimelineEntry() {
		name = "Name Goes Here";
	}

	@SuppressWarnings("NegativelyNamedBooleanVariable")
	public CustomTimelineEntry(
			double time,
			@Nullable String name,
			@Nullable Pattern sync,
			@Nullable Double duration,
			@NotNull TimelineWindow timelineWindow,
			@Nullable Double jump,
			@Nullable URL icon,
			@Nullable TimelineReference replaces,
			 boolean disabled,
			 boolean callout,
			 double calloutPreTime
	) {
		// TODO: this wouldn't be a bad place to do the JAR url correction. Perhaps not the cleanest way,
		// but it works.
		this.time = time;
		this.name = name;
		this.sync = sync;
		this.callout = callout;
		this.calloutPreTime = calloutPreTime;
		this.duration = duration;
		this.windowStart = timelineWindow.start();
		this.windowEnd = timelineWindow.end();
		this.jump = jump;
		this.icon = icon;
		this.replaces = replaces;
		this.enabled = !disabled;
	}

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public CustomTimelineEntry(
			@JsonProperty("time") double time,
			@JsonProperty("name") @Nullable String name,
			@JsonProperty("sync") @Nullable String sync,
			@JsonProperty("duration") @Nullable Double duration,
			@JsonProperty("timelineWindow") @NotNull TimelineWindow timelineWindow,
			@JsonProperty("jump") @Nullable Double jump,
			@JsonProperty("icon") @Nullable URL icon,
			@JsonProperty("replaces") @Nullable TimelineReference replaces,
			@JsonProperty(value = "disabled", defaultValue = "false") boolean disabled,
			@JsonProperty(value = "callout", defaultValue = "false") boolean callout,
			@JsonProperty(value = "calloutPreTime", defaultValue = "0") double calloutPreTime,
			@JsonProperty(value = "jobs") @Nullable CombatJobSelection jobs
	) {
		// TODO: this wouldn't be a bad place to do the JAR url correction. Perhaps not the cleanest way,
		// but it works.
		this.time = time;
		this.name = name;
		this.sync = sync == null ? null : Pattern.compile(sync, Pattern.CASE_INSENSITIVE);
		this.callout = callout;
		this.calloutPreTime = calloutPreTime;
		this.duration = duration;
		this.windowStart = timelineWindow.start();
		this.windowEnd = timelineWindow.end();
		this.jump = jump;
		this.icon = icon;
		this.replaces = replaces;
		this.enabled = !disabled;
		this.enabledJobs = jobs == null ? CombatJobSelection.all() : jobs;
	}

	@Override
	@JsonProperty
	public double time() {
		return time;
	}

	@Override
	@JsonProperty
	public @Nullable String name() {
		return name;
	}

	@Override
	@JsonProperty
	public @Nullable Pattern sync() {
		return sync;
	}

	@Override
	@JsonProperty
	public @Nullable Double duration() {
		return duration;
	}

	@Override
	@JsonProperty
	public @NotNull TimelineWindow timelineWindow() {
		Double start = windowStart;
		Double end = windowEnd;
		if (start == null) {
			start = 2.5;
		}
		if (end == null) {
			end = 2.5;
		}
		return new TimelineWindow(start, end);
	}

	@Override
	@JsonProperty
	public @Nullable Double jump() {
		return jump;
	}

	@Override
	@JsonProperty
	public URL icon() {
		return icon;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomTimelineEntry that = (CustomTimelineEntry) o;
		Pattern thisSync = sync();
		Pattern thatSync = that.sync();
		return Objects.equals(time(), that.time())
				&& Objects.equals(name(), that.name())
				&& Objects.equals(thisSync == null ? null : thisSync.pattern(), thatSync == null ? null : thatSync.pattern())
				&& Objects.equals(jump(), that.jump())
				&& Objects.equals(duration(), that.duration())
				&& Objects.equals(icon(), that.icon())
				&& Objects.equals(timelineWindow(), that.timelineWindow());
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, name, sync, duration, windowStart, windowEnd, jump, icon, replaces, enabled);
	}

	@JsonProperty
	@Override
	public @Nullable TimelineReference replaces() {
		return replaces;
	}

	@Override
	public boolean enabled() {
		return enabled;
	}

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	public boolean disabled() {
		return !enabled;
	}

	public static CustomTimelineEntry overrideFor(TimelineEntry other) {
		return new CustomTimelineEntry(
				other.time(),
				other.name(),
				other.sync(),
				other.duration(),
				other.timelineWindow(),
				other.jump(),
				other.icon(),
				new TimelineReference(other.time(), other.name(), other.sync() == null ? null : other.sync().pattern()),
				false,
				false,
				0
		);
	}

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	@Override
	public boolean callout() {
		return callout;
	}

	@Override
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	public double calloutPreTime() {
		return calloutPreTime;
	}

	@Override
	public boolean enabledForJob(Job job) {
		return enabledJobs.enabledForJob(job);
	}

	@JsonProperty("jobs")
	public @Nullable CombatJobSelection getEnabledJobs() {
		// Don't bother serializing if every job is selected
		return enabledJobs.isEnabledForAll() ? null : enabledJobs;
	}
}
