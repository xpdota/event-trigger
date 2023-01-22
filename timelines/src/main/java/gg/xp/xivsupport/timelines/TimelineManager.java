package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.PlayerChangedJobEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.lang.LanguageController;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;
import gg.xp.xivsupport.timelines.intl.TimelineReplacements;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class TimelineManager {

	private static final Logger log = LoggerFactory.getLogger(TimelineManager.class);
	private static final Map<Long, TimelineInfo> zoneIdToTimelineFile = new HashMap<>();
	private static final ObjectMapper mapper = new ObjectMapper();
	private final BooleanSetting debugMode;
	private final BooleanSetting prePullShow;
	private final BooleanSetting resetOnMapChange;
	private final XivState state;
	private final LanguageController lang;
	private final PersistenceProvider pers;
	private final EventMaster master;
	private final IntSetting rowsToDisplay;
	private final IntSetting secondsPast;
	private final IntSetting secondsFuture;
	private final ModifiableCallout<TimelineProcessor.UpcomingCall> timelineTriggerCalloutNow = ModifiableCallout.<TimelineProcessor.UpcomingCall>durationBasedCallWithoutDurationText("Timeline Callout (Immediate)", "{event.getEntry().name()}")
			.guiProvider(e -> IconTextRenderer.getStretchyIcon(e.getIconUrl()));
	private final ModifiableCallout<TimelineProcessor.UpcomingCall> timelineTriggerCalloutPre = ModifiableCallout.<TimelineProcessor.UpcomingCall>durationBasedCall("Timeline Callout (Precall)", "{event.getEntry().name()}")
			.guiProvider(e -> IconTextRenderer.getStretchyIcon(e.getIconUrl()));

	private TimelineProcessor currentTimeline;
	private XivZone zone;

	public TimelineManager(EventMaster master, PersistenceProvider pers, XivState state, LanguageController lang) {
		this.master = master;
		rowsToDisplay = new IntSetting(pers, "timeline-overlay.max-displayed", 6, 1, 32);
		secondsPast = new IntSetting(pers, "timeline-overlay.seconds-past", 0, 0, null);
		secondsFuture = new IntSetting(pers, "timeline-overlay.seconds-future", 60, 0, null);
		debugMode = new BooleanSetting(pers, "timeline-overlay.debug-mode", false);
		prePullShow = new BooleanSetting(pers, "timeline-overlay.show-pre-pull", false);
		resetOnMapChange = new BooleanSetting(pers, "timeline-overlay.reset-on-map-change", false);
		this.state = state;
		this.lang = lang;
		ModifiedCalloutHandle.installHandle(timelineTriggerCalloutNow, pers, "timeline-support.trigger-call-now");
		ModifiedCalloutHandle.installHandle(timelineTriggerCalloutPre, pers, "timeline-support.trigger-call-pre");
		this.pers = pers;
		new Thread(TimelineManager::ensureInit, "TimelineInitHelper").start();
	}

	private static volatile boolean init;
	private static final Object initLock = new Object();

	private static void ensureInit() {
		if (!init) {
			synchronized (initLock) {
				if (!init) {
					log.info("Timeline init start");
					TimelineCsvReader.readCsv().forEach(entry -> {
						zoneIdToTimelineFile.put(entry.zoneId(), entry);
					});
					init = true;
					log.info("Timeline init done");
				}
			}
		}
	}

	public @Nullable TimelineProcessor getTimeline(long zoneId) {
		ensureInit();
		TimelineInfo info = zoneIdToTimelineFile.get(zoneId);
		if (info == null) {
			log.info("No timeline info for zone {}", zoneId);
			return null;
		}
		String filename = info.filename();
		if (filename == null || filename.isBlank()) {
			log.info("No timeline found for zone {}", zoneId);
			return null;
		}
		InputStream resource = TimelineManager.class.getResourceAsStream("/timeline/" + filename);
		if (resource == null) {
			log.info("Timeline file '{}' for zone '{}' is missing", filename, zoneId);
			return null;
		}
		InputStream generalTranslationStream = TimelineManager.class.getResourceAsStream("/timeline/translations/global_timeline_replacements.txt.json");
		InputStream specificTranslationsStream = TimelineManager.class.getResourceAsStream("/timeline/translations/" + filename + ".json");

		LanguageReplacements combined = LanguageReplacements.combine(
				Stream.of(generalTranslationStream, specificTranslationsStream)
						.filter(Objects::nonNull)
						.map(translationsStream -> {
									LanguageReplacements lr;
									try {
										TimelineReplacements tr = mapper.readValue(translationsStream, TimelineReplacements.class);
										String lang = this.lang.getGameLanguage().getShortCode();
										lr = tr.langs().getOrDefault(lang, LanguageReplacements.empty());
										log.info("Timeline translation: {} ({} name translations and {} sync translations)", lang, lr == null ? 0 : lr.replaceText().size(), lr == null ? 0 : lr.replaceSync().size());
										return lr;
									}
									catch (IOException e) {
										log.error("Error loading timeline translations for zone {}", zoneId, e);
										return null;
									}
								}
						)
						.filter(Objects::nonNull)
						.toList());

		try {
			return TimelineProcessor.of(this, resource, getCustomEntries(zoneId), state.getPlayerJob(), combined);
		}
		catch (Throwable e) {
			log.error("Error loading timeline for zone {}", zoneId, e);
			return null;
		}

	}

	public List<? extends TimelineEntry> getCustomEntries(long zoneId) {
		return getCustomSettings(zoneId).getEntries();
	}

	private static String propStubForZoneId(long zoneId) {
		return String.format("timeline.custom.zone-%s.custom-entries", zoneId);
	}

	private final Map<Long, TimelineCustomizations> customizations = new ConcurrentHashMap<>();

	public TimelineCustomizations getCustomSettings(long zoneId) {
		return customizations.computeIfAbsent(zoneId, (k) -> pers.get(propStubForZoneId(k), TimelineCustomizations.class, new TimelineCustomizations()));
	}

	public void commitCustomSettings(long zoneId) {
		TimelineCustomizations cust = customizations.get(zoneId);
		if (cust == null) {
			log.warn("Customization was not found for zone {}", zoneId);
		}
		else {
			pers.save(propStubForZoneId(zoneId), cust);
		}
		if (this.zone != null && this.zone.getId() == zoneId && currentTimeline != null) {
			resetCurrentTimelineKeepSync();
		}
	}

	private void resetCurrentTimelineKeepSync() {
		if (this.zone != null && currentTimeline != null) {
			// TODO: save/restore the timestamp so it is easier to live-edit
			TimelineProcessor.TimelineSync lastSync = currentTimeline.getLastSync();
			loadTimelineForCurrentZone();
			currentTimeline.setLastSync(lastSync);
		}
	}

	@HandleEvents
	public void changeJob(EventContext context, PlayerChangedJobEvent jobChange) {
		resetCurrentTimelineKeepSync();
	}

	@HandleEvents
	public void changeZone(EventContext context, ZoneChangeEvent zoneChangeEvent) {
		XivZone zone = zoneChangeEvent.getZone();
		doZoneChange(zone);
	}

	@HandleEvents(order = 40_000)
	public void actLine(EventContext context, ACTLogLineEvent event) {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline != null) {
			currentTimeline.processActLine(event);
		}
	}

	@HandleEvents(order = 40_000)
	public void mapChange(EventContext context, MapChangeEvent event) {
		if (resetOnMapChange.get()) {
			resetCurrent();
		}
	}

	@HandleEvents(order = 40_000)
	public void newPull(EventContext context, PullStartedEvent event) {
		resetCurrent();
	}

	// TODO: "Pull Ended" event?
	@HandleEvents(order = 40_000)
	public void pullEnded(EventContext context, VictoryEvent event) {
		resetCurrent();
	}

	@HandleEvents(order = 40_000)
	public void inCombatStatusChange(EventContext context, InCombatChangeEvent icce) {
		if (!icce.isInCombat()) {
			resetCurrent();
		}
	}

	private void resetCurrent() {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline != null) {
			currentTimeline.reset();
		}
	}

	private void doZoneChange(XivZone zoneId) {
		this.zone = zoneId;
		loadTimelineForCurrentZone();
	}

	private void loadTimelineForCurrentZone() {
		if (zone == null) {
			log.info("Zone is null, doing nothing");
			return;
		}
		long zoneId = zone.getId();
		currentTimeline = getTimeline(zoneId);
		if (currentTimeline == null) {
			log.info("No timeline for zone '{}'", zoneId);
		}
		else {
			log.info("Loaded timeline for zone '{}', {} timeline entries", zoneId, currentTimeline.getEntries().size());
		}
	}

	public static Map<Long, TimelineInfo> getTimelines() {
		ensureInit();
		return Collections.unmodifiableMap(zoneIdToTimelineFile);
	}

	public List<VisualTimelineEntry> getCurrentDisplayEntries() {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline == null) {
			return Collections.emptyList();
		}
		return currentTimeline.getCurrentTimelineEntries();
	}

	void doTriggerCall(TimelineProcessor.UpcomingCall upc) {
		RawModifiedCallout<TimelineProcessor.UpcomingCall> event;
		if (upc.isPreCall()) {
			event = timelineTriggerCalloutPre.getModified(upc);
		}
		else {
			event = timelineTriggerCalloutNow.getModified(upc);
		}
		master.pushEvent(event);
	}


	public IntSetting getRowsToDisplay() {
		return rowsToDisplay;
	}

	public BooleanSetting getDebugMode() {
		return debugMode;
	}

	public IntSetting getSecondsFuture() {
		return secondsFuture;
	}

	public IntSetting getSecondsPast() {
		return secondsPast;
	}

	public BooleanSetting getPrePullSetting() {
		return prePullShow;
	}

	public BooleanSetting getResetOnMapChangeSetting() {
		return resetOnMapChange;
	}
}
