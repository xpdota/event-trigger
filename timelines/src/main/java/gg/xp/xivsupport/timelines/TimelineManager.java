package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimelineManager {

	private static final Logger log = LoggerFactory.getLogger(TimelineManager.class);
	private static final Map<Long, TimelineInfo> zoneIdToTimelineFile = new HashMap<>();
	private final BooleanSetting debugMode;
	private final BooleanSetting prePullShow;
	private final BooleanSetting resetOnMapChange;
	private final PersistenceProvider pers;
	private final EventMaster master;
	private final IntSetting rowsToDisplay;
	private final IntSetting secondsPast;
	private final IntSetting secondsFuture;
	private final ModifiableCallout<TimelineProcessor.UpcomingCall> timelineTriggerCalloutNow = ModifiableCallout.durationBasedCallWithoutDurationText("Timeline Callout (Immediate)", "{event.getEntry().name()}");
	private final ModifiableCallout<TimelineProcessor.UpcomingCall> timelineTriggerCalloutPre = ModifiableCallout.durationBasedCall("Timeline Callout (Precall)", "{event.getEntry().name()}");

	private TimelineProcessor currentTimeline;
	private XivZone zone;

	public TimelineManager(EventMaster master, PersistenceProvider pers) {
		this.master = master;
		rowsToDisplay = new IntSetting(pers, "timeline-overlay.max-displayed", 6, 1, 32);
		secondsPast = new IntSetting(pers, "timeline-overlay.seconds-past", 0, 0, null);
		secondsFuture = new IntSetting(pers, "timeline-overlay.seconds-future", 60, 0, null);
		debugMode = new BooleanSetting(pers, "timeline-overlay.debug-mode", false);
		prePullShow = new BooleanSetting(pers, "timeline-overlay.show-pre-pull", false);
		resetOnMapChange = new BooleanSetting(pers, "timeline-overlay.reset-on-map-change", false);
		ModifiedCalloutHandle.installHandle(timelineTriggerCalloutNow, pers, "timeline-support.trigger-call-now");
		ModifiedCalloutHandle.installHandle(timelineTriggerCalloutPre, pers, "timeline-support.trigger-call-pre");
		this.pers = pers;
	}

	private static volatile boolean init;
	private static final Object initLock = new Object();

	private static void ensureInit() {
		if (!init) {
			synchronized (initLock) {
				if (!init) {
					TimelineCsvReader.readCsv().forEach(entry -> {
						zoneIdToTimelineFile.put(entry.zoneId(), entry);
					});
					init = true;
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
		try {
			return TimelineProcessor.of(this, resource, getCustomEntries(zoneId));
		}
		catch (Throwable e) {
			log.error("Error loading timeline", e);
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

	public void commitCustomSettings(long zoneId)  {
		TimelineCustomizations cust = customizations.get(zoneId);
		if (cust == null) {
			log.warn("Customization was not found for zone {}", zoneId);
		}
		else {
			pers.save(propStubForZoneId(zoneId), cust);
		}
		if (this.zone != null && this.zone.getId() == zoneId && currentTimeline != null) {
			// TODO: save/restore the timestamp so it is easier to live-edit
			TimelineProcessor.TimelineSync lastSync = currentTimeline.getLastSync();
			loadTimelineForCurrentZone();
			currentTimeline.setLastSync(lastSync);
		}
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
		CalloutEvent event;
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
