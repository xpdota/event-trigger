package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.CurrentTimeSource;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
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
import gg.xp.xivsupport.persistence.settings.FileSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongListSetting;
import gg.xp.xivsupport.timelines.cbevents.CbEventType;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;
import gg.xp.xivsupport.timelines.intl.TimelineReplacements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class TimelineManager {

	private static final Logger log = LoggerFactory.getLogger(TimelineManager.class);
	private static final Map<Long, TimelineInfo> zoneIdToTimelineFile = new HashMap<>();
	private static final ObjectMapper mapper = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.build();
	public static final String CUSTOMIZATION_EXPORT_SOURCE = "Exported Customization";
	private final CurrentTimeSource timeSource;
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
	private final ModifiableCallout<TimelineProcessor.UpcomingTrigger> timelineTriggerCalloutNow = ModifiableCallout.<TimelineProcessor.UpcomingTrigger>durationBasedCallWithoutDurationText("Timeline Callout (Immediate)", "{event.getEntry().name()}")
			.guiProvider(e -> IconTextRenderer.getStretchyIcon(e.getIconUrl()));
	private final ModifiableCallout<TimelineProcessor.UpcomingTrigger> timelineTriggerCalloutPre = ModifiableCallout.<TimelineProcessor.UpcomingTrigger>durationBasedCall("Timeline Callout (Precall)", "{event.getEntry().name()}")
			.guiProvider(e -> IconTextRenderer.getStretchyIcon(e.getIconUrl()));
	private final FileSetting cactbotUserDirSetting;
	private final IntSetting barTimeBasis;
	private final IntSetting barWidth;
	private final LongListSetting customZoneIds;

	private TimelineProcessor currentTimeline;
	private XivZone zone;

	public TimelineManager(PicoContainer pico, EventMaster master, PersistenceProvider pers, XivState state, LanguageController lang) {
		CurrentTimeSource ts = pico.getComponent(CurrentTimeSource.class);
		this.timeSource = ts == null ? Instant::now : ts;
		this.master = master;
		rowsToDisplay = new IntSetting(pers, "timeline-overlay.max-displayed", 6, 1, 32);
		secondsPast = new IntSetting(pers, "timeline-overlay.seconds-past", 0, 0, null);
		secondsFuture = new IntSetting(pers, "timeline-overlay.seconds-future", 60, 0, null);
		debugMode = new BooleanSetting(pers, "timeline-overlay.debug-mode", false);
		prePullShow = new BooleanSetting(pers, "timeline-overlay.show-pre-pull", false);
		resetOnMapChange = new BooleanSetting(pers, "timeline-overlay.reset-on-map-change", false);
		barWidth = new IntSetting(pers, "timeline-bar-width", 150, 50, 1000);
		barTimeBasis = new IntSetting(pers, "timeline-overlay.bar-time-basis-seconds", 60, 1, 3600);
		customZoneIds = new LongListSetting(pers, "timeline.custom.fresh-zone-ids", new long[0]);

		File defaultUserDir;
		try {
			defaultUserDir = Path.of(System.getenv("APPDATA"), "Advanced Combat Tracker", "Plugins", "cactbot", "cactbot", "user").toFile();
		}
		catch (Throwable t) {
			log.warn("Error initializing default cactbot user dir location");
			defaultUserDir = new File(".");
		}
		cactbotUserDirSetting = new FileSetting(pers, "cactbot-integration.user-dir-location", defaultUserDir);
		this.state = state;
		this.lang = lang;
		ModifiedCalloutHandle.installHandle(timelineTriggerCalloutNow, pers, "timeline-support.trigger-call-now");
		ModifiedCalloutHandle.installHandle(timelineTriggerCalloutPre, pers, "timeline-support.trigger-call-pre");
		this.pers = pers;
		new Thread(TimelineManager::ensureInit, "TimelineInitHelper").start();
		customZoneIds.addListener(this::resetCurrentTimelineKeepSync);
	}

	private static volatile boolean init;
	private static final Object initLock = new Object();

	private static void ensureInit() {
		if (!init) {
			synchronized (initLock) {
				if (!init) {
					log.info("Timeline init start");
					TimelineCsvReader.readCsv().forEach(entry -> zoneIdToTimelineFile.put(entry.zoneId(), entry));
					init = true;
					log.info("Timeline init done");
				}
			}
		}
	}

	public @Nullable TimelineInfo getInfoForZone(long zoneId) {
		ensureInit();
		return zoneIdToTimelineFile.get(zoneId);
	}

	public @Nullable TimelineProcessor getTimelineIfEnabled(long zoneId) {
		TimelineCustomizations cust = getCustomSettings(zoneId);
		if (!cust.enabled) {
			log.info("Timeline disabled for zone {}", zoneId);
			return null;
		}
		return getTimeline(zoneId);
	}

	private @NotNull TimelineProcessor getTimelineCustom(long zoneId) {
		// TODO: pre-fill this with some standard entries
		TimelineCustomizations cust = getCustomSettings(zoneId);
		InputStream dummy = new ByteArrayInputStream(new byte[0]);
		return TimelineProcessor.of(this, dummy, cust.getEntries(), state.getPlayerJob(), LanguageReplacements.empty());
	}

	private @Nullable TimelineProcessor getTimelineReal(long zoneId) {
		TimelineInfo info = getInfoForZone(zoneId);
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
										log.debug("Timeline translation: {} ({} name translations and {} sync translations)", lang, lr == null ? 0 : lr.replaceText().size(), lr == null ? 0 : lr.replaceSync().size());
										return lr;
									}
									catch (Throwable e) {
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

	public @Nullable TimelineProcessor getTimeline(long zoneId) {
		TimelineProcessor real = getTimelineReal(zoneId);
		if (real != null) {
			return real;
		}
		if (customZoneIds.get().contains(zoneId)) {
			return getTimelineCustom(zoneId);
		}
		return null;
	}

	public List<? extends TimelineEntry> getCustomEntries(long zoneId) {
		return getCustomSettings(zoneId).getEntries();
	}

	private static String propStubForZoneId(long zoneId) {
		return String.format("timeline.custom.zone-%s.custom-entries", zoneId);
	}

	private final Map<Long, TimelineCustomizations> customizations = new ConcurrentHashMap<>();

	public TimelineCustomizations getCustomSettings(long zoneId) {
		try {
			return customizations.computeIfAbsent(zoneId, (k) -> pers.get(propStubForZoneId(k), TimelineCustomizations.class, new TimelineCustomizations()));
		}
		catch (Throwable t) {
			throw new RuntimeException("Error loading timeline customizations for zone " + zoneId, t);
		}
	}

	public void commitCustomSettings(long zoneId) {
		TimelineCustomizations cust = customizations.get(zoneId);
		if (cust == null) {
			log.warn("Customization was not found for zone {}", zoneId);
		}
		else {
			pers.save(propStubForZoneId(zoneId), cust);
		}
		if (this.zone != null && this.zone.getId() == zoneId) {
			resetCurrentTimelineKeepSync();
		}
	}

	private void resetCurrentTimelineKeepSync() {
		if (this.zone != null) {
			if (currentTimeline != null) {
				// Save/restore the timestamp so it is easier to live-edit
				TimelineProcessor.TimelineSync lastSync = currentTimeline.getLastSync();
				loadTimelineForCurrentZone();
				if (currentTimeline != null) {
					currentTimeline.setLastSync(lastSync);
				}
			}
			else {
				// This can happen if you have just enabled the timeline
				loadTimelineForCurrentZone();
			}
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
	public void actLine(EventContext context, BaseEvent event) {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline != null) {
			currentTimeline.processEvent(event);
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
		currentTimeline = getTimelineIfEnabled(zoneId);
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

	public Map<Long, TimelineInfo> getCustomTimelines() {
		List<Long> zoneIds = customZoneIds.get();
		if (zoneIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, TimelineInfo> out = new HashMap<>();
		Map<Long, TimelineInfo> nonCustom = getTimelines();
		for (Long zoneId : zoneIds) {
			if (!nonCustom.containsKey(zoneId)) {
				out.put(zoneId, new TimelineInfo(zoneId, "Custom"));
			}
		}
		return out;
	}

	public boolean addCustomZone(long zoneId) {
		// Don't allow adding if there's already a non-custom zone
		if (getInfoForZone(zoneId) != null || customZoneIds.get().contains(zoneId)) {
			return false;
		}
		boolean added = customZoneIds.mutate(l -> l.add(zoneId));
		if (added) {
			TimelineCustomizations custom = getCustomSettings(zoneId);
			CustomTimelineEntry reset = new CustomTimelineEntry();
			reset.name = "--reset--";
			reset.time = 0.0;
			CustomEventSyncController esc = new CustomEventSyncController(CbEventType.InCombat, Map.of("inACTCombat", new ArrayList<>(List.of("1"))));
			reset.esc = esc;
			custom.setEntries(List.of(reset));
		}
		return added;
	}

	public boolean removeCustomZone(long zoneId) {
		return customZoneIds.mutate(l -> l.remove(zoneId));
	}

	public List<VisualTimelineEntry> getCurrentDisplayEntries() {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline == null) {
			return Collections.emptyList();
		}
		return currentTimeline.getCurrentTimelineEntries();
	}

	void doTriggerCall(TimelineProcessor.UpcomingTrigger upc) {
		RawModifiedCallout<TimelineProcessor.UpcomingTrigger> event;
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

	public FileSetting cactbotDirSetting() {
		return cactbotUserDirSetting;
	}

	public IntSetting getBarTimeBasis() {
		return barTimeBasis;
	}

	public IntSetting getBarWidth() {
		return barWidth;
	}

	public TimelineProcessor getCurrentProcessor() {
		return this.currentTimeline;
	}

	public CurrentTimeSource getTimeSource() {
		return this.timeSource;
	}

	public TimelineCustomizationExport exportCustomizations(Collection<Long> zones) {
		if (zones.isEmpty()) {
			throw new IllegalArgumentException("Must specify at least one zone");
		}
		TimelineCustomizationExport out = TimelineCustomizationExport.empty();
		zones.forEach(z -> out.timelineCustomizations.put(z, getCustomSettings(z)));
		return out;
	}

	public String serializeCurrentCustomizations(Set<Long> zones) {
		try {
			return mapper.writeValueAsString(exportCustomizations(zones));
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	public TimelineCustomizationExport deserializeCustomizations(String serialized) {
		try {
			return mapper.readValue(serialized, TimelineCustomizationExport.class);
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	public ZoneTimelineDescription describeZone(long zoneId) {
		TimelineInfo info = getInfoForZone(zoneId);
		if (info == null) {
			return new ZoneTimelineDescription(zoneId, null);
		}
		else {
			return new ZoneTimelineDescription(zoneId, info.filename());
		}
	}

}
