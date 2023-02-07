package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.CurrentTimeSource;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.state.floormarkers.FloorMarker;
import gg.xp.xivsupport.events.state.floormarkers.FloorMarkerRepository;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.sys.Threading;
import org.java_websocket.util.NamedThreadFactory;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ScanMe
public class MapDataController {

	private static final Logger log = LoggerFactory.getLogger(MapDataController.class);

	private final XivState realState;
	private final ActiveCastRepository realAcr;
	private final StatusEffectRepository realStatuses;
	private final SequenceIdTracker sqid;
	private final ExecutorService exs = Executors.newSingleThreadExecutor(Threading.namedDaemonThreadFactory("MapDataController"));
	private static final Snapshot initialEmptySnapshot = new Snapshot(
			Instant.EPOCH,
			XivMap.UNKNOWN,
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyMap(),
			Collections.emptyMap(),
			Collections.emptyMap(),
			Collections.emptyMap()
	);
	private final BooleanSetting enableCapture;
	private final FloorMarkerRepository floorMarkers;
	private final IntSetting maxCaptures;
	private final IntSetting msBetweenCaptures;
	// TODO: is timeBasis still needed with this?
	private final CurrentTimeSource timeSource;
	private List<Snapshot> snapshots = new ArrayList<>();

	{
		snapshots.add(initialEmptySnapshot);
	}

	private volatile int index;
	private volatile boolean live = true;
	private Runnable callback = () -> {
	};
	private BaseEvent timeBasis;
	private volatile long lastTimestamp;

	public MapDataController(XivState state,
	                         ActiveCastRepository acr,
	                         StatusEffectRepository statuses,
	                         SequenceIdTracker sqid,
	                         PersistenceProvider pers,
	                         PicoContainer container,
	                         RightClickOptionRepo rc,
	                         FloorMarkerRepository floorMarkers
	) {
		realState = state;
		realAcr = acr;
		// TODO: statuses need the same treatment as cast bars - we need to fake the time
		realStatuses = statuses;
		this.sqid = sqid;
		this.enableCapture = new BooleanSetting(pers, "map-replay.record-data", false);
		this.floorMarkers = floorMarkers;
		CurrentTimeSource timeSource = container.getComponent(CurrentTimeSource.class);
		this.timeSource = timeSource == null ? Instant::now : timeSource;
		enableCapture.addAndRunListener(() -> {
			live = true;
			boolean enabled = enableCapture.get();
			if (enabled) {
				clearAll();
			}
			callback.run();
		});
		maxCaptures = new IntSetting(pers, "map-replay.max-snapshots", 50_000, 100, 2_000_000_000);
		msBetweenCaptures = new IntSetting(pers, "map-replay.capture-interval", 200, 0, 10_000);
		rc.addOption(CustomRightClickOption.forRow("Seek Map Tab", Event.class, event -> {
			Instant effectiveHappenedAt = event.getEffectiveHappenedAt();
			// TODO: replace this with binary search
			for (int i = 0; i < snapshots.size(); i++) {
				Snapshot snap = snapshots.get(i);
				if (snap.time != null && snap.time.isAfter(effectiveHappenedAt)) {
					setIndex(i);
					MapTab map = container.getComponent(MapTab.class);
					if (map != null) {
						GuiUtil.bringToFront(map);
					}
					return;
				}
			}
		}));
	}

	// record to capture all the data used by the map panel
	private record Snapshot(
			Instant time,
			XivMap map,
			List<XivPlayerCharacter> partyList,
			List<XivCombatant> combatants,
			Map<Long, List<BuffApplied>> statuses,
			Map<Long, CastTracker> casts,
			Map<Long, Long> pendingDamage,
			Map<FloorMarker, Position> floorMarkers

	) {
	}

	private Snapshot getLast() {
		return snapshots.get(snapshots.size() - 1);
	}

	private Snapshot getCurrent() {
		// TODO: have 'live' mode just bypass this entirely
		if (live) {
			return snapshots.get(snapshots.size() - 1);
		}
		else {
			return snapshots.get(index);
		}
	}

	public void setLive(boolean live) {
		log.info("setLive({})", live);
		if (!live) {
			this.index = snapshots.size() - 1;
		}
		this.live = live;
		callback.run();
	}

	public void setIndex(int index) {
		log.trace("setIndex({})", index);
		this.index = index;
		live = false;
		callback.run();
	}

	public void setRelativeIndex(int delta) {
		if (delta == 0) {
			return;
		}
		int newIndex = live ? snapshots.size() - 1 : index + delta;
		if (newIndex < 0) {
			newIndex = 0;
		}
		else if (newIndex >= snapshots.size()) {
			newIndex = snapshots.size() - 1;
		}
		log.trace("setRelativeIndex({}) => {}", delta, newIndex);
		setIndex(newIndex);
	}

	public void setRelativeIndexAutoLive(int delta) {
		if (delta == 0) {
			return;
		}
		if (live && delta > 0) {
			return;
		}
		int newIndex = live ? snapshots.size() - 1 : index + delta;
		if (newIndex < 0) {
			newIndex = 0;
		}
		if (newIndex >= snapshots.size()) {
			newIndex = snapshots.size() - 1;
			live = true;
		}
		else {
			live = false;
		}
		index = newIndex;
		log.trace("setRelativeIndexAutoLive({}) => {}", delta, newIndex);
		callback.run();
	}


	public void clearAll() {
		log.info("Clearing replay");
		live = true;
		Snapshot last = getLast();
		List<Snapshot> newList = new ArrayList<>();
		newList.add(last);
		snapshots = newList;
		callback.run();
	}

	public int getIndex() {
		return live ? snapshots.size() - 1 : index;
	}

	public boolean isLive() {
		return live;
	}

	public int getSize() {
		return snapshots.size();
	}

	public void setCallback(Runnable callback) {
		this.callback = callback;
	}

	public void captureSnapshot() {
		if (!enableCapture.get()) {
			return;
		}
		int ms = msBetweenCaptures.get();
		if (ms > 0) {
			long now = timeSource.now().toEpochMilli();
			long delta = now - lastTimestamp;
			if (delta < ms) {
				return;
			}
			lastTimestamp = now;
		}
		Snapshot snap = getLast();
		List<XivPlayerCharacter> partyList = realState.getPartyList();
		// We can't dedup the combatant list wholly because
		List<XivCombatant> newCombatantsList = realState.getCombatantsListCopy();
		List<XivCombatant> oldCombatantsList = snap.combatants;
		final Map<Long, Long> pendingDamage = new HashMap<>(newCombatantsList.size());
		for (XivCombatant xivCombatant : newCombatantsList) {
			long damage = sqid.unresolvedDamageOnEntity(xivCombatant);
			if (damage > 0) {
				pendingDamage.put(xivCombatant.getId(), damage);
			}
		}
		List<CastTracker> rawCasts = realAcr.getAll();
		exs.submit(() -> {
			List<XivCombatant> newEffectiveCombatantsList;
			if (newCombatantsList.size() == oldCombatantsList.size()) {
				newEffectiveCombatantsList = oldCombatantsList;
				for (int i = 0; i < newCombatantsList.size(); i++) {
					if (!combatantEquals(oldCombatantsList.get(i), newCombatantsList.get(i))) {
						newEffectiveCombatantsList = newCombatantsList;
						break;
					}
				}
			}
			else {
				newEffectiveCombatantsList = newCombatantsList;
			}
			Map<Long, CastTracker> casts = new HashMap<>(rawCasts.size());
			for (CastTracker rawCast : rawCasts) {
				if (timeBasis == null) {
					timeBasis = rawCast.getCast();
				}
				casts.put(rawCast.getCast().getSource().getId(), rawCast);
			}
			Instant time;
			if (timeBasis != null) {
				time = timeBasis.effectiveTimeNow();
			}
			else {
				time = timeSource.now();
			}
			List<BuffApplied> rawBuffs = realStatuses.getBuffs();
			Map<Long, List<BuffApplied>> oldBuffs = snap.statuses;
			Map<Long, List<BuffApplied>> buffs = new HashMap<>(rawBuffs.size());
			Map<FloorMarker, Position> floorMarkers = this.floorMarkers.getMarkers();
			for (BuffApplied rawBuff : rawBuffs) {
				buffs.computeIfAbsent(rawBuff.getTarget().getId(), unused -> new ArrayList<>()).add(rawBuff);
			}
			snapshots.add(new Snapshot(
					time,
					realState.getMap(),
					dedup(partyList, snap.partyList),
					// Can't dedup this since they're keyed strictly on ID
					newEffectiveCombatantsList,
//				dedup(combatantsListCopy, snap.combatants),
					dedup(buffs, oldBuffs),
					dedup(casts, snap.casts),
					pendingDamage.isEmpty() ? Collections.emptyMap() : dedup(pendingDamage, snap.pendingDamage),
					dedup(floorMarkers, snap.floorMarkers)
			));
			checkLength();
		});
	}

	// This is implicitly thread-safe (at least from a write standpoint) because it runs in a single thread executor
	private void checkLength() {
		int oldSize = snapshots.size();
		if (oldSize > maxCaptures.get()) {
			log.info("Truncating replay");
			// Truncate 10%
			int truncation = (int) (oldSize * 0.10);
			List<Snapshot> newList = new ArrayList<>(snapshots.subList(truncation, oldSize));
			snapshots = newList;
			if (index > newList.size()) {
				index = newList.size() - 1;
			}
		}
	}

	// Dedup old vs new values
	private static <X> X dedup(X newValue, X oldValue) {
		if (Objects.equals(newValue, oldValue)) {
			return oldValue;
		}
		else {
			return newValue;
		}
	}

	private static boolean combatantEquals(XivCombatant newCbt, XivCombatant oldCbt) {
		return Objects.equals(newCbt.getId(), oldCbt.getId())
				&& Objects.equals(newCbt.getPos(), oldCbt.getPos())
				&& Objects.equals(newCbt.getHp(), oldCbt.getHp())
				&& Objects.equals(newCbt.getMp(), oldCbt.getMp())
				&& Objects.equals(newCbt.getShieldAmount(), oldCbt.getShieldAmount());
	}


	public List<XivCombatant> getCombatants() {
		if (live) {
			return realState.getCombatantsListCopy();
		}
		return Collections.unmodifiableList(getCurrent().combatants());
	}

	public List<BuffApplied> buffsOnCombatant(XivEntity cbt) {
		if (live) {
			return realStatuses.statusesOnTarget(cbt);
		}
		Instant time = getCurrent().time;
		List<BuffApplied> buffs = getCurrent().statuses.get(cbt.getId());
		if (buffs == null || buffs.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			if (time == null) {
				return Collections.unmodifiableList(buffs);
			}
			return buffs.stream().map(ba -> ba.withNewCurrentTime(time)).toList();
		}
//		return realStatuses.statusesOnTarget(cbt);
	}

	public XivMap getMap() {
		if (live) {
			return realState.getMap();
		}
		return getCurrent().map;
//		return realState.getMap();
	}

	public @Nullable CastTracker getCastFor(XivCombatant cbt) {
		if (live) {
			return realAcr.getCastFor(cbt);
		}
		Instant time = getCurrent().time;
		CastTracker tracker = getCurrent().casts.get(cbt.getId());
		if (tracker == null) {
			return null;
		}
		else if (time == null) {
			return tracker;
		}
		else {
			return tracker.withNewCurrentTime(time);
		}
//		return realAcr.getCastFor(cbt);
	}

	public List<XivPlayerCharacter> getPartyList() {
		if (live) {
			return realState.getPartyList();
		}
		return Collections.unmodifiableList(getCurrent().partyList);
//		return realState.getPartyList();
	}

	public long unresolvedDamage(XivCombatant xivCombatant) {
		if (live) {
			return sqid.unresolvedDamageOnEntity(xivCombatant);
		}
		else {
			return getCurrent().pendingDamage.getOrDefault(xivCombatant.getId(), 0L);
		}
	}

	public Map<FloorMarker, Position> getFloorMarkers() {
		if (live) {
			return floorMarkers.getMarkers();
		}
		else {
			Map<FloorMarker, Position> markers = getCurrent().floorMarkers;
			if (markers.isEmpty()) {
				return Collections.emptyMap();
			}
			return new EnumMap<>(markers);
		}
	}

	public Instant getTime() {
		return getCurrent().time();
	}

	// Settings
	public BooleanSetting getEnableCapture() {
		return enableCapture;
	}

	public IntSetting getMaxCaptures() {
		return maxCaptures;
	}

	public IntSetting getMsBetweenCaptures() {
		return msBetweenCaptures;
	}
}
