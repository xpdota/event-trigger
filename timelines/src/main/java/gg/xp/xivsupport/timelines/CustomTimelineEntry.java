package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.timelines.icon.IconIdTimelineIcon;
import gg.xp.xivsupport.timelines.icon.TimelineIcon;
import gg.xp.xivsupport.timelines.icon.UrlTimelineIcon;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Just going to use jackson for now
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = CustomTimelineEntry.class)
public class CustomTimelineEntry implements CustomTimelineItem, Serializable {

	@Serial
	private static final long serialVersionUID = 8590938155631598982L;
	private static final Logger log = LoggerFactory.getLogger(CustomTimelineEntry.class);
	// TODO: encapsulate these better
	public double time;
	public @Nullable String name;
	public @Nullable Pattern sync;
	public @Nullable CustomEventSyncController esc;
	public @Nullable Double duration;
	public @Nullable Double windowStart;
	public @Nullable Double windowEnd;
	public @Nullable Double jump;
	public @Nullable String jumpLabel;
	public @Nullable String importSource;
	public boolean forceJump;
	// TODO: this uses the absolute path to the JAR, which means icons will break if the user moves their install location.
	// Best solution is to probably make our own little class that lets you specify an ability/status ID in addition to
	// plain URLs.
	// This also means that we would need to retain old icons if they are replaced.
	public @Nullable TimelineIcon iconSpec;
	private @Nullable TimelineReference replaces;
	public boolean enabled = true;
	public boolean callout;
	public double calloutPreTime;
	public CombatJobSelection enabledJobs = CombatJobSelection.all();

	public CustomTimelineEntry() {
		name = "Name Goes Here";
	}

	@Contract("null -> null; !null -> !null")
	private static @Nullable TimelineIcon convertLegacyIcon(@Nullable URL legacyIcon) {
		if (legacyIcon == null) {
			return null;
		}
		try {
			String path = legacyIcon.getPath();
			Pattern legacyIconPattern = Pattern.compile("(\\d{6})_hr1.png");
			Matcher matcher = legacyIconPattern.matcher(path);
			if (matcher.find()) {
				int id = Integer.parseInt(matcher.group(1));
				return new IconIdTimelineIcon(id);
			}
		} catch (Throwable t) {
			log.warn("Failed to parse legacyIcon '{}'", legacyIcon, t);
		}
		return new UrlTimelineIcon(legacyIcon);
	}

	// OLD Constructor used for creating fresh entries
	@SuppressWarnings("NegativelyNamedBooleanVariable")
	@Deprecated
	public CustomTimelineEntry(
			double time,
			@Nullable String name,
			@Nullable Pattern sync,
			@Nullable CustomEventSyncController esc,
			@Nullable Double duration,
			@NotNull TimelineWindow timelineWindow,
			@Nullable Double jump,
			@Nullable String jumpLabel,
			@Nullable Boolean forceJump,
			@Nullable URL legacyIcon,
			@Nullable TimelineReference replaces,
			boolean disabled,
			boolean callout,
			double calloutPreTime,
			@Nullable String importSource
	) {
		// TODO: this wouldn't be a bad place to do the JAR url correction. Perhaps not the cleanest way,
		// but it works.
		this(time, name, sync, esc, duration, timelineWindow, jump, jumpLabel, forceJump, convertLegacyIcon(legacyIcon), replaces, disabled, callout, calloutPreTime, importSource);
	}

	// NEW Constructor used for creating fresh entries
	@SuppressWarnings("NegativelyNamedBooleanVariable")
	public CustomTimelineEntry(
			double time,
			@Nullable String name,
			@Nullable Pattern sync,
			@Nullable CustomEventSyncController esc,
			@Nullable Double duration,
			@NotNull TimelineWindow timelineWindow,
			@Nullable Double jump,
			@Nullable String jumpLabel,
			@Nullable Boolean forceJump,
			@Nullable TimelineIcon iconSpec,
			@Nullable TimelineReference replaces,
			boolean disabled,
			boolean callout,
			double calloutPreTime,
			@Nullable String importSource
	) {
		// TODO: this wouldn't be a bad place to do the JAR url correction. Perhaps not the cleanest way,
		// but it works.
		this.time = time;
		this.name = name;
		this.sync = sync;
		this.esc = esc;
		this.callout = callout;
		this.calloutPreTime = calloutPreTime;
		this.duration = duration;
		this.windowStart = timelineWindow.start();
		this.windowEnd = timelineWindow.end();
		this.jump = jump;
		this.jumpLabel = jumpLabel;
		this.forceJump = forceJump != null && forceJump;
		this.iconSpec = iconSpec;
		this.replaces = replaces;
		this.enabled = !disabled;
		this.importSource = importSource;
	}

	@Deprecated // Old JSON constructor
	public CustomTimelineEntry(
			@JsonProperty("time") double time,
			@JsonProperty("name") @Nullable String name,
			@JsonProperty("sync") @Nullable String sync,
			@JsonProperty("esc") @Nullable CustomEventSyncController esc,
			@JsonProperty("duration") @Nullable Double duration,
			@JsonProperty("timelineWindow") @NotNull TimelineWindow timelineWindow,
			@JsonProperty("jump") @Nullable Double jump,
			@JsonProperty("jumplabel") @Nullable String jumpLabel,
			@JsonProperty("forcejump") @Nullable Boolean forceJump,
			@JsonProperty("icon") @Nullable URL legacyIcon,
			@JsonProperty("replaces") @Nullable TimelineReference replaces,
			@JsonProperty(value = "disabled", defaultValue = "false") boolean disabled,
			@JsonProperty("importSource") String importSource,
			@JsonProperty(value = "callout", defaultValue = "false") boolean callout,
			@JsonProperty(value = "calloutPreTime", defaultValue = "0") double calloutPreTime,
			@JsonProperty("jobs") @Nullable CombatJobSelection jobs
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
		this.jumpLabel = jumpLabel;
		this.forceJump = forceJump != null && forceJump;
		iconSpec = convertLegacyIcon(legacyIcon);
		this.replaces = replaces;
		this.enabled = !disabled;
		this.enabledJobs = jobs == null ? CombatJobSelection.all() : jobs;
		this.esc = esc;
		this.importSource = importSource;
	}

	// NEW json constructor
	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public CustomTimelineEntry(
			@JsonProperty("time") double time,
			@JsonProperty("name") @Nullable String name,
			@JsonProperty("sync") @Nullable String sync,
			@JsonProperty("esc") @Nullable CustomEventSyncController esc,
			@JsonProperty("duration") @Nullable Double duration,
			@JsonProperty("timelineWindow") @NotNull TimelineWindow timelineWindow,
			@JsonProperty("jump") @Nullable Double jump,
			@JsonProperty("jumplabel") @Nullable String jumpLabel,
			@JsonProperty("forcejump") @Nullable Boolean forceJump,
			@JsonProperty("icon") @Nullable URL legacyIcon,
			@JsonProperty("iconSpec") @Nullable TimelineIcon newIcon,
			@JsonProperty("replaces") @Nullable TimelineReference replaces,
			@JsonProperty(value = "disabled", defaultValue = "false") boolean disabled,
			@JsonProperty("importSource") String importSource,
			@JsonProperty(value = "callout", defaultValue = "false") boolean callout,
			@JsonProperty(value = "calloutPreTime", defaultValue = "0") double calloutPreTime,
			@JsonProperty("jobs") @Nullable CombatJobSelection jobs
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
		this.jumpLabel = jumpLabel;
		this.forceJump = forceJump != null && forceJump;
		if (newIcon != null) {
			iconSpec = newIcon;
		}
		else {
			iconSpec = convertLegacyIcon(legacyIcon);
		}
		this.replaces = replaces;
		this.enabled = !disabled;
		this.enabledJobs = jobs == null ? CombatJobSelection.all() : jobs;
		this.esc = esc;
		this.importSource = importSource;
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
	public @Nullable String jumpLabel() {
		return jumpLabel;
	}

	@Override
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
//	@JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IncludeTrue.class)
	public boolean forceJump() {
		return forceJump;
	}

	@JsonProperty
	public @Nullable TimelineIcon iconSpec() {
		return iconSpec;
	}

	@Override
	@JsonIgnore // This is captured in iconSpec now
	public @Nullable URL icon() {
		TimelineIcon icon = this.iconSpec;
		if (icon == null) {
			return null;
		}
		return icon.getIconUrl();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomTimelineEntry that = (CustomTimelineEntry) o;
		boolean syncEquals = Objects.equals(sync == null ? null : sync.pattern(), that.sync == null ? that.sync : that.sync.pattern());
		return Double.compare(that.time, time) == 0 && forceJump == that.forceJump && enabled == that.enabled && callout == that.callout && Double.compare(that.calloutPreTime, calloutPreTime) == 0 && Objects.equals(name, that.name) && syncEquals && Objects.equals(duration, that.duration) && Objects.equals(windowStart, that.windowStart) && Objects.equals(windowEnd, that.windowEnd) && Objects.equals(jump, that.jump) && Objects.equals(jumpLabel, that.jumpLabel) && Objects.equals(iconSpec, that.iconSpec) && Objects.equals(replaces, that.replaces) && Objects.equals(getEnabledJobs(), that.getEnabledJobs());
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, name, sync, duration, windowStart, windowEnd, jump, jumpLabel, forceJump, iconSpec, replaces, enabled, callout, calloutPreTime, getEnabledJobs());
	}

	@Override
	@JsonProperty("esc")
	public @Nullable EventSyncController eventSyncController() {
		return esc;
	}

	@Override
	public String toString() {
		return "CustomTimelineEntry{" +
		       "time=" + time +
		       ", name='" + name + '\'' +
		       ", sync=" + sync +
		       ", esc=" + esc +
		       ", duration=" + duration +
		       ", windowStart=" + windowStart +
		       ", windowEnd=" + windowEnd +
		       ", jump=" + jump +
		       ", jumpLabel='" + jumpLabel + '\'' +
		       ", forceJump=" + forceJump +
		       ", icon=" + iconSpec +
		       ", replaces=" + replaces +
		       ", enabled=" + enabled +
		       ", callout=" + callout +
		       ", calloutPreTime=" + calloutPreTime +
		       ", enabledJobs=" + enabledJobs +
		       '}';
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

	@Override
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	public boolean isLabel() {
		return false;
	}

	public static CustomTimelineEntry overrideFor(TimelineEntry other) {
		if (other.isLabel()) {
			throw new IllegalArgumentException("Cannot override a label with a real entry");
		}
		EventSyncController otherEsc = other.eventSyncController();
		CustomTimelineEntry newCte = new CustomTimelineEntry(
				other.time(),
				other.name(),
				other.sync(),
				otherEsc == null ? null : CustomEventSyncController.from(otherEsc),
				other.duration(),
				other.timelineWindow(),
				other.jump(),
				other.jumpLabel(),
				other.forceJump(),
				other.icon(),
				TimelineReference.of(other),
				false,
				false,
				0,
				null
		);
		newCte.enabledJobs = jobSelFor(other);
		return newCte;
	}

	public static CustomTimelineEntry cloneFor(TimelineEntry other) {
		EventSyncController otherEsc = other.eventSyncController();
		CustomTimelineEntry newCte = new CustomTimelineEntry(
				other.time(),
				other.name() + " copy",
				other.sync(),
				otherEsc == null ? null : CustomEventSyncController.from(otherEsc),
				other.duration(),
				other.timelineWindow(),
				other.jump(),
				other.jumpLabel(),
				other.forceJump(),
				other.icon(),
				null,
				false,
				other.callout(),
				other.calloutPreTime(),
				null
		);
		newCte.enabledJobs = jobSelFor(other);
		return newCte;
	}

	private static CombatJobSelection jobSelFor(TimelineEntry other) {
		if (other instanceof CustomTimelineEntry cte) {
			return cte.enabledJobs.copy();
		}
		else {
			return CombatJobSelection.all();
		}
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

	@Override
	public @Nullable String getImportSource() {
		return importSource;
	}

	@Override
	public void setImportSource(@Nullable String importSource) {
		this.importSource = importSource;
	}
}
