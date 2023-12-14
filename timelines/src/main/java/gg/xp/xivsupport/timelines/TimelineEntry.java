package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Base interface for a timeline entry
 */
public interface TimelineEntry extends Comparable<TimelineEntry> {

	/**
	 * @return The earliest possible sync time
	 */
	@JsonIgnore
	default double getMinTime() {
		return time() - timelineWindow().start();
	}

	/**
	 * @return The latest possible sync time
	 */
	@JsonIgnore
	default double getMaxTime() {
		return time() + timelineWindow().end();
	}


	/**
	 * Determine whether the timeline should be synced to this entry given the current time and a line
	 *
	 * @param currentTime The time at which the timeline currently sits
	 * @param line        The incoming log line
	 * @return Whether the timeline should sync
	 */
	default boolean shouldSync(double currentTime, String line) {
		Pattern sync = sync();
		if (sync == null) {
			return false;
		}
		boolean timesMatch = (currentTime >= getMinTime() && currentTime <= getMaxTime());
		if (!timesMatch) {
			return false;
		}
		return sync.matcher(line).find();
	}

	@Nullable EventSyncController eventSyncController();

	default boolean hasEventSync() {
		return eventSyncController() != null;
	};

	default @Nullable Class<? extends Event> eventSyncType() {
		EventSyncController esc = eventSyncController();
		return esc == null ? null : esc.eventType();
	}

	default boolean shouldSync(double currentTime, Event event) {
		EventSyncController syncControl = eventSyncController();
		if (syncControl == null) {
			return false;
		}
		boolean timesMatch = (currentTime >= getMinTime() && currentTime <= getMaxTime());
		if (!timesMatch) {
			return false;
		}
		return syncControl.shouldSync(event);
	}

	/**
	 * @return true if this timeline entry would ever cause a sync
	 */
	default boolean canSync() {
		return sync() != null || hasEventSync();
	}

	/**
	 * @return The time to sync to, or null in the case of a resolution error (error should be logged elsewhere)
	 */
	@JsonIgnore
	default Double getSyncToTime(LabelResolver resolver) {
		Double jump = jump();
		if (jump == null) {
			String jumpLabel = jumpLabel();
			if (jumpLabel == null) {
				return time();
			}
			else {
				return resolver.resolve(jumpLabel);
			}
		}
		else {
			return jump;
		}

	}

	@Override
	@Nullable String toString();

	/**
	 * The time of this timeline entry
	 *
	 * @return The timeline entry's time
	 */
	double time();

	/**
	 * The name of this timeline entry.
	 * <p>
	 * For most entries, this is the displayed name. For labels, it is the label name.
	 * For triggers, it is the text that will be displayed on-screen.
	 *
	 * @return The name
	 */
	@Nullable String name();

	/**
	 * ACT parsed line regex to sync to
	 *
	 * @return The sync pattern
	 */
	@Nullable Pattern sync();

	/**
	 * An optional duration for the bar to be in "active" state as opposed to immediately
	 * becoming "in the past" when its time arrives.
	 *
	 * @return The optional duration
	 */
	@Nullable Double duration();

	/**
	 * The relative window in which this entry can be synced to
	 *
	 * @return The sync window
	 */
	@NotNull TimelineWindow timelineWindow();

	/**
	 * An optional time to jump to. If {@link #forceJump()} is false, this line must have its
	 * sync conditions hit (i.e. we are syncing onto this line). If forceJump is true, then
	 * the jump will also happen if this timeline entry's time is hit.
	 *
	 * @return The jump time.
	 * @see #forceJump()
	 */
	@Nullable Double jump();

	/**
	 * Same as {@link #jump()}, but takes a 'label' to jump to instead of a raw time.
	 *
	 * @return The jump label
	 * @see #forceJump()
	 */
	@Nullable String jumpLabel();

	/**
	 * Affects the behavior of {@link #jump()} and {@link #jumpLabel()}. If true, then the jump
	 * will occur unconditionally when the current timeline time hits the time of this entry
	 * (i.e. when we are T-0 from hitting this entry). If false, jumps will only occur when this
	 * entry is synced to.
	 *
	 * @return whether to force jumps even without a sync
	 * @see #jump()
	 * @see #jumpLabel()
	 */
	default boolean forceJump() {
		return false;
	}

	/**
	 * @return true if this entry is a label
	 */
	default boolean isLabel() {
		return false;
	}

	/**
	 * @return Whether this entry is enabled
	 */
	boolean enabled();

	/**
	 * Compare the times of this timeline entry vs another entry
	 *
	 * @param o the timeline entry to which we are comparing
	 * @return The same as {@code Double.compare(this.time(), o.time())}
	 */
	@Override
	default int compareTo(@NotNull TimelineEntry o) {
		return Double.compare(this.time(), o.time());
	}

	/**
	 * @return An optional icon to display for the entry on the timeline bar and trigger (if it has one)
	 */
	default @Nullable URL icon() {
		return null;
	}

	/**
	 * @return If this entry overrides another entry, return a reference to the entry it overrides. Otherwise, return null.
	 * @see TimelineReference
	 */
	default @Nullable TimelineReference replaces() {
		return null;
	}

	/**
	 * Determine if another timeline entry should be replaced by this one. Always returns null if
	 * {@link #replaces()} is null.
	 *
	 * @param that The entry to check if we are replacing.
	 * @return true if it should be superseded/replaced by this entry, false otherwise.
	 */
	default boolean shouldSupersede(TimelineEntry that) {
		TimelineReference overrides = this.replaces();
		if (overrides == null) {
			return false;
		}
		// To allow users to switch languages freely without losing their overrides,
		// compare to the untranslated versions.
		that = that.untranslated();
		// Rules:
		// 1. Time must match
		// 2. Name must match
		// 3. Sync must match
		// 4. For backwards compatibility, treat replacing an empty/null pattern as always matching for step 2
		String desiredName = overrides.name();
		//noinspection FloatingPointEquality
		if (overrides.time() == that.time()) {
			String thatPattern = that.sync() == null ? null : that.sync().pattern();
			if (!Objects.equals(desiredName, that.name())) {
				return false;
			}
			if (overrides.pattern() == null || overrides.pattern().isBlank()) {
				return true;
			}
			return (Objects.equals(overrides.pattern(), thatPattern));
		}
		return false;
	}

	/**
	 * @return True if this entry is also a timeline trigger.
	 */
	boolean callout();

	/**
	 * @return The amount of time before this entry's {@link #time()} that the trigger should fire
	 */
	double calloutPreTime();

	/**
	 * @return The effective time at which the trigger would fire
	 */
	default double effectiveCalloutTime() {
		return time() - calloutPreTime();
	}

	/**
	 * Whether the entry is enabled for a certain job
	 *
	 * @param job The job
	 * @return true if enabled for that job
	 */
	default boolean enabledForJob(Job job) {
		return true;
	}

	/**
	 * The untranslated version of this entry.
	 *
	 * @return The untranslated version. Returns 'this' if it is already untranslated.
	 */
	default TimelineEntry untranslated() {
		return this;
	}

	/**
	 * @return This entry, ported back to Cactbot format.
	 */
	@JsonIgnore
	default String toTextFormat() {
		StringBuilder sb = new StringBuilder();
		if (!enabled()) {
			// If disabled, just comment out the line
			sb.append("# ");
		}
		sb.append(fmtDouble(time())).append(' ');
		if (isLabel()) {
			sb.append("label ").append('"').append(name()).append('"');
		}
		else {
			String name = name();
			if (name == null || name.isEmpty()) {
				sb.append("\"--sync--\"");
			}
			else {
				sb.append('"').append(name).append('"');
			}
			sb.append(' ');
			if (sync() != null) {
				sb.append("sync /").append(sync().pattern()).append('/').append(' ');
			}
			if (eventSyncController() != null) {
				sb.append(eventSyncController().toTextFormat()).append(' ');
			}
			TimelineWindow window = timelineWindow();
			if (!TimelineWindow.DEFAULT.equals(window)) {
				sb.append("window ").append(fmtDouble(window.start())).append(',').append(fmtDouble(window.end())).append(' ');
			}
			Double duration = duration();
			if (duration != null) {
				sb.append("duration ").append(fmtDouble(duration)).append(' ');
			}
			Double jump = jump();
			String jumpKeyword = forceJump() ? "forcejump" : "jump";
			if (jump != null) {
				sb.append(jumpKeyword).append(' ').append(fmtDouble(jump));
			}
			else if (jumpLabel() != null) {
				sb.append(jumpKeyword).append(' ').append('"').append(jumpLabel()).append('"');
			}
		}
		return sb.toString();
	}

	/**
	 * @return Any extra Cactbot-format timeline entries needed to make this work as a trigger. Does not include the
	 * actual user-js trigger, only the timeline entries that will be necessary for it.
	 */
	@JsonIgnore
	default Stream<String> makeTriggerTimelineEntries() {
		if (!enabled() || !callout()) {
			return Stream.empty();
		}
		String uniqueName = makeUniqueName();
		String hideAllLine = "hideall \"%s\"".formatted(uniqueName);
		String actualTimelineLine = new TextFileTimelineEntry(time(), uniqueName, null, null, TimelineWindow.DEFAULT, null, null, false, null).toTextFormat();
		return Stream.of(hideAllLine, actualTimelineLine);
	}

	/**
	 * @return A stream consisting of {@link #toTextFormat()} and {@link #makeTriggerTimelineEntries()}
	 */
	@JsonIgnore
	default Stream<String> getAllTextEntries() {
		return Stream.concat(Stream.of(toTextFormat()), makeTriggerTimelineEntries());
	}

	@JsonIgnore
	private static String fmtDouble(double dbl) {
		// Use US locale to force period as the decimal separator
		return String.format(Locale.US, "%.01f", dbl);
	}

	@JsonIgnore
	private String makeUniqueName() {
		// And then use DE here to use a comma instead since . is special in regex
		return String.format(Locale.GERMANY, "timeline-trig@%.02f", time());
	}

	@JsonIgnore
	private String callTextForExport() {
		String callText = name();
		if (callText == null) {
			callText = "";
		}
		return callText.replaceAll("\"", "'");
	}

	/**
	 * @return The Cactbot user-js needed to make this timeline trigger work. If this is not a trigger, return null;
	 */
	@JsonIgnore
	default @Nullable String makeTriggerJs() {
		if (!callout()) {
			return null;
		}
		String uniqueName = makeUniqueName();
		return String.format("""
						{
							id: "%s",
							suppressSeconds: 1,
							regex: /%s/,
							beforeSeconds: %s,
							alertText: "%s",
						},
				""", uniqueName, uniqueName, calloutPreTime(), callTextForExport());
	}
}
