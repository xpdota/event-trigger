package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.regex.Pattern;

public class TranslatedTextFileEntry implements TimelineEntry {

	private final TimelineEntry untranslated;
	private final @Nullable String nameOverride;
	private final @Nullable Pattern syncOverride;
	private final @Nullable EventSyncController esc;

	public TranslatedTextFileEntry(TimelineEntry untranslated, @Nullable String nameOverride, @Nullable Pattern syncOverride, @Nullable EventSyncController esc) {
		this.untranslated = untranslated;
		this.nameOverride = nameOverride;
		this.syncOverride = syncOverride;
		this.esc = esc;
	}

	@Override
	@JsonIgnore
	public double getMinTime() {
		return untranslated.getMinTime();
	}

	@Override
	@JsonIgnore
	public double getMaxTime() {
		return untranslated.getMaxTime();
	}

	@Override
	public @Nullable EventSyncController eventSyncController() {
		return esc ;
	}

	@Override
	@JsonIgnore
	public Double getSyncToTime(LabelResolver resolver) {
		return untranslated.getSyncToTime(resolver);
	}

	@Override
	@Nullable
	public String jumpLabel() {
		return untranslated.jumpLabel();
	}

	@Override
	public boolean forceJump() {
		return untranslated.forceJump();
	}

	@Override
	public String toString() {
		return "TranslatedTextFileEntry{" +
		       "nameOverride='" + nameOverride +
		       "', syncOverride='" + syncOverride +
		       "', syncControl='" + esc +
		       "', untranslated='" + untranslated +
		       "'}";
	}

	@Override
	public double time() {
		return untranslated.time();
	}

	@Override
	@Nullable
	public String name() {
		return nameOverride == null ? untranslated.name() : nameOverride;
	}

	@Override
	@Nullable
	public Pattern sync() {
		return syncOverride == null ? untranslated.sync() : syncOverride;
	}

	@Override
	@Nullable
	public Double duration() {
		return untranslated.duration();
	}

	@Override
	@NotNull
	public TimelineWindow timelineWindow() {
		return untranslated.timelineWindow();
	}

	@Override
	@Nullable
	public Double jump() {
		return untranslated.jump();
	}

	@Override
	public boolean enabled() {
		return untranslated.enabled();
	}

	@Override
	public int compareTo(@NotNull TimelineEntry o) {
		return untranslated.compareTo(o);
	}

	@Override
	public @Nullable URL icon() {
		return untranslated.icon();
	}

	@Override
	@Nullable
	public TimelineReference replaces() {
		return untranslated.replaces();
	}

	@Override
	public boolean shouldSupersede(TimelineEntry that) {
		return untranslated.shouldSupersede(that);
	}

	@Override
	public boolean callout() {
		return untranslated.callout();
	}

	@Override
	public double calloutPreTime() {
		return untranslated.calloutPreTime();
	}

	@Override
	public double effectiveCalloutTime() {
		return untranslated.effectiveCalloutTime();
	}

	@Override
	public boolean enabledForJob(Job job) {
		return untranslated.enabledForJob(job);
	}

	@Override
	public TimelineEntry untranslated() {
		return untranslated;
	}
}
