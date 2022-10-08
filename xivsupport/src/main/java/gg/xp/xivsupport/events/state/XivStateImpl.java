package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.CurrentTimeSource;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.XivMap;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.OnlineStatus;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.RawOnlineStatusChanged;
import gg.xp.xivsupport.events.actlines.events.RawPlayerChangeEvent;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.sys.EnhancedReadWriteReentrantLock;
import gg.xp.xivsupport.sys.LockAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XivStateImpl implements XivState {

	private static final Logger log = LoggerFactory.getLogger(XivStateImpl.class);
	private final EventMaster master;
	private final PartySortOrder pso;
	private final @Nullable CurrentTimeSource fakeTimeSource;

	private XivZone zone;
	private XivMap map = XivMap.UNKNOWN;
	// EARLY player info before we have combatant data
	private XivEntity playerPartial;
	// For override
	private XivEntity playerTmpOverride;
	// TODO: see if same-world parties with out-of-zone players would break this
	private volatile @NotNull List<RawXivPartyInfo> partyListRaw = Collections.emptyList();
	private volatile @Nullable List<Long> partyListForceOrder;
	private volatile @NotNull List<XivPlayerCharacter> partyListProcessed = Collections.emptyList();

	private Job lastPlayerJob;

	public XivStateImpl(EventMaster master, PartySortOrder pso, PicoContainer pico) {
		this.master = master;
		this.pso = pso;
		fakeTimeSource = pico.getComponent(CurrentTimeSource.class);
		// This might technically have a very slight concurrency issue, but it's doubtful
		// that it would ever become a real issue.
		pso.addListener(this::recalcState);
	}

	// Note: can be null until we have all the required data, but this should only happen very early on in init

	/**
	 * @return The current local player, or null if we don't have a player yet
	 */
	@Override
	public XivPlayerCharacter getPlayer() {
		CombatantData data = getData(getPlayerId());
		XivCombatant cbt = data != null ? data.getComputed() : null;
		if (cbt instanceof XivPlayerCharacter xpc) {
			return xpc;
		}
		return null;
	}

	/**
	 * @param player Set the current player. Realistically, only the ID matters here.
	 */
	public void setPlayer(XivEntity player) {
		log.info("Player changed to {}", player);
		this.playerPartial = player;
		recalcState();
	}

	/**
	 * Set a temporary (as in, not persisted across app starts) override for who to consider the primary player.
	 *
	 * @param player The new primary player. Only the ID matters.
	 */
	public void setPlayerTmpOverride(XivEntity player) {
		this.playerTmpOverride = player;
		recalcState();
	}

	// Note: can be null until we've seen a 01-line

	/**
	 * @return The current zone. May be null until the data has been seen.
	 */
	@Override
	public XivZone getZone() {
		return zone;
	}

	/**
	 * @return The current map. Very likely to be null until a map change.
	 */
	@Override
	public XivMap getMap() {
		return map;
	}

	/**
	 * Set the current zone
	 *
	 * @param zone The new zone
	 */
	public void setZone(XivZone zone) {
		log.info("Zone changed to {}", zone);
		this.zone = zone;
	}

	/**
	 * Set the current map
	 *
	 * @param map The new map
	 */
	public void setMap(XivMap map) {
		log.info("Map changed to {}", map);
		this.map = map;
	}

	/**
	 * Set the party list
	 *
	 * @param partyList Party info like what you would get from OP WS
	 */
	public void setPartyList(List<RawXivPartyInfo> partyList) {
		this.partyListRaw = new ArrayList<>(partyList);
		recalcState();
		log.info("Party list changed to {}", this.partyListProcessed.stream().map(XivEntity::getName).collect(Collectors.joining(", ")));
	}

	/**
	 * @return The current party. Note that unlike ACT/OP, this will report the primary player as being in the party
	 * even if you aren't actually in a party.
	 */
	@Override
	public List<XivPlayerCharacter> getPartyList() {
		return new ArrayList<>(partyListProcessed);
	}

	private long getPlayerId() {
		if (playerTmpOverride != null) {
			return playerTmpOverride.getId();
		}
		else if (playerPartial != null) {
			return playerPartial.getId();
		}
		else {
			// These shouldn't both be null for very long, so just wait until we get the data.
			return -1;
		}
	}

	private boolean recomputeAll() {
		boolean changed = false;
		try (LockAdapter ignored = lock.read()) {
			for (CombatantData data : combatantData.values()) {
				changed = data.recomputeIfDirty() || changed;
			}
		}
		return changed;
	}

	// TODO: emit events after state is recalculated reflecting actual changes
	// Probably requires proper equals/hashcode on everything so we can actually compare them
	private void recalcState() {

		try (LockAdapter ignored = lock.write()) {
			// First, move removed stuff from the main map to the graveyard
			Iterator<Map.Entry<Long, CombatantData>> iter = combatantData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Long, CombatantData> next = iter.next();
				if (next.getValue().isRemoved()) {
					graveyard.put(next.getKey(), new WeakReference<>(next.getValue().getComputed()));
					iter.remove();
				}
			}
			// Then, give everything an initial pass
			recomputeAll();
//		if (!changed) {
//			return;
//		}
			Map<Long, List<CombatantData>> combatantsByNpcName = new HashMap<>();
			combatantData.values().forEach(c -> {
				XivCombatant computed = c.getComputed();
				long ownerId = computed.getOwnerId();
				if (ownerId != 0) {
					c.setOwner(getCombatant(ownerId));
				}
				if (computed.getType() == CombatantType.NPC && computed.getLevel() < 70) {
					combatantsByNpcName.computeIfAbsent(computed.getbNpcNameId(), (ignore) -> new ArrayList<>()).add(c);
				}
			});
			combatantsByNpcName.forEach((name, values) -> {
				if (values.size() <= 2) {
					// Skip if only one, nothing to do
					// With two, unfortunately we can't really confirm if it's a fake or not,
					// because we don't have any other alleged fakes to compare it to.
					return;
				}
				// Sort highest HP first
				values.sort(Comparator.<CombatantData, Long>comparing(npc -> npc.getComputed().getHp() != null ? npc.getComputed().getHp().max() : 0).reversed());
				XivCombatant primaryCombatant = values.get(0).getComputed();
				if (primaryCombatant.getHp() == null) {
					return;
				}
				List<CombatantData> potentialFakes = new ArrayList<>();
				for (CombatantData otherCombatant : values.subList(1, values.size())) {
					XivCombatant computed = otherCombatant.getComputed();
					if (computed.getHp() == null) {
						continue;
					}
					if (computed.getHp().max() < primaryCombatant.getHp().max()
							&& computed.getbNpcId() != primaryCombatant.getbNpcId()) {
						potentialFakes.add(otherCombatant);
					}
				}
				if (potentialFakes.size() >= 2) {
					XivCombatant firstPossibleFake = potentialFakes.get(0).getComputed();
					boolean allMatch = potentialFakes.subList(1, potentialFakes.size()).stream().allMatch(p -> p.getComputed().getPos() != null && p.getComputed().getPos().equals(firstPossibleFake.getPos()));
					if (allMatch) {
						potentialFakes.forEach(fake -> {
							fake.setFake(true);
							// Also use Parent field to link to real NPC if it doesn't already have a real owner
							if (fake.owner == null) {
								fake.setOwner(primaryCombatant);
							}
						});
					}
				}
			});
			// TODO: just doing a simple diff of this would be a great way to synthesize
			// add/remove combatant events
			List<XivPlayerCharacter> partyListProcessed;
			List<Long> forceOrder = partyListForceOrder;
			if (forceOrder == null) {
				partyListProcessed = new ArrayList<>(partyListRaw.size());
				partyListRaw.forEach(rawPartyMember -> {
					if (!rawPartyMember.isInParty()) {
						// Member of different alliance, ignore
						// TODO: is there value in supporting alliance stuff?
						return;
					}
					long id = rawPartyMember.getId();
					CombatantData data = getOrCreateData(id);
					data.setFromPartyInfo(rawPartyMember);
					XivCombatant fullCombatant = data.getComputed();
					if (fullCombatant instanceof XivPlayerCharacter xpc) {
						partyListProcessed.add(xpc);
					}
					else {
						log.warn("Party member was not a PC? {}", fullCombatant);
					}
				});
				partyListProcessed.sort(Comparator.comparing(p -> {
					if (getPlayerId() == p.getId()) {
						// Always sort main player first
						return -1;
					}
					else {
						return pso.getSortOrder(p.getJob());
					}
				}));
			}
			else {
				partyListProcessed = forceOrder
						.stream()
						.map(this::getOrCreateData)
						.map(CombatantData::getComputed)
						.filter(XivPlayerCharacter.class::isInstance)
						.map(XivPlayerCharacter.class::cast)
						.toList();
			}
			combatantCache = combatantData.values().stream()
					.filter(CombatantData::includeInList)
					.peek(CombatantData::recomputeIfDirty)
					.collect(Collectors.toMap(CombatantData::getId, CombatantData::getComputed));
			XivPlayerCharacter player = getPlayer();
			if (partyListProcessed.isEmpty() && player != null) {
				this.partyListProcessed = List.of(player);
			}
			else {
				this.partyListProcessed = partyListProcessed;
			}
			log.trace("Recalculated state, player is {}, party is {}", player, partyListProcessed);
			// TODO: improve this
			if (master != null) {
				// TODO: this is kind of a workaround for tests, should improve it
				XivStateRecalculatedEvent event = new XivStateRecalculatedEvent();
				if (fakeTimeSource != null) {
					event.setTimeSource(fakeTimeSource);
					event.setHappenedAt(fakeTimeSource.now());
				}
				master.getQueue().push(event);
				if (player != null) {
					Job newJob = player.getJob();
					if (lastPlayerJob != newJob) {
						master.getQueue().push(new PlayerChangedJobEvent(lastPlayerJob, newJob));
					}
					lastPlayerJob = newJob;
				}
			}
		}
	}

	/**
	 * Check whether the current zone matches the given zone ID
	 *
	 * @param zoneId Zone ID to check against
	 * @return true/false if it matches
	 */
	@Override
	public boolean zoneIs(long zoneId) {
		XivZone zone = getZone();
		return zone != null && zone.getId() == zoneId;
	}

	/**
	 * Provide state info for ALL combatants. Anything that was seen previously that is not in this list will be
	 * considered to have been removed.
	 *
	 * @param combatants The list of all combatants
	 */
	public void setCombatants(List<RawXivCombatantInfo> combatants) {
		Set<Long> processed = new HashSet<>(combatants.size());
		combatants.forEach(combatant -> {
			long id = combatant.getId();
			if (id == 0xE0000000L) {
				return;
			}
			boolean alreadyProcessed = !processed.add(id);
			if (alreadyProcessed) {
				log.warn("Duplicate combatant data for id {}: ({})", id, combatant);
			}
			CombatantData data = getOrCreateData(id);
			data.setRaw(combatant);
		});
		combatantData.values().forEach(cbtInfo -> {
			if (!processed.contains(cbtInfo.getId())) {
				cbtInfo.setRemoved(true);
			}
		});
		log.trace("Received info on {} combatants", combatants.size());
		recalcState();
	}

	public void setSpecificCombatants(List<RawXivCombatantInfo> combatants) {
		combatants.forEach(combatant -> {
			long id = combatant.getId();
			if (id == 0xE0000000L) {
				return;
			}
			CombatantData data = getOrCreateData(id);
			data.setRaw(combatant);
		});
		log.trace("Received info on {} combatants", combatants.size());
		recalcState();
	}

	@Override
	public void removeSpecificCombatant(long idToRemove) {
		getOrCreateData(idToRemove).setRemoved(true);
		recalcState();
	}

	@Override
	public Map<Long, XivCombatant> getCombatants() {
		return Collections.unmodifiableMap(combatantCache);
	}

	// TODO: does this still need to be a copy?
	@Override
	public List<XivCombatant> getCombatantsListCopy() {
		return new ArrayList<>(combatantCache.values());
	}

	@Override
	public int getPartySlotOf(XivEntity entity) {
		List<XivPlayerCharacter> partyList = getPartyList();
		return IntStream.range(0, partyList.size())
				.filter(i -> partyList.get(i).getId() == entity.getId())
				.findFirst()
				.orElse(-1);
	}

	private boolean dirtyOverrides;

	@Override
	public void provideCombatantHP(XivCombatant target, @NotNull HitPoints hitPoints) {
		getOrCreateData(target.getId()).setHpOverride(hitPoints);
		dirtyOverrides = true;
	}

	@Override
	public void provideCombatantMP(XivCombatant target, @NotNull ManaPoints manaPoints) {
		getOrCreateData(target.getId()).setMpOverride(manaPoints);
		dirtyOverrides = true;
	}

	@Override
	public void provideCombatantPos(XivCombatant target, Position newPos) {
		getOrCreateData(target.getId()).setPosOverride(newPos);
		dirtyOverrides = true;
	}

	@Override
	public void provideCombatantShieldPct(XivCombatant target, long shieldPct) {
		getOrCreateData(target.getId()).setShieldPct(shieldPct);
		dirtyOverrides = true;
	}

	@Override
	public void provideActFallbackCombatant(XivCombatant cbt) {
		getOrCreateData(cbt.getId()).setFromOtherActLine(cbt);
		dirtyOverrides = true;
	}

	@Override
	public void flushProvidedValues() {
		if (dirtyOverrides) {
			dirtyOverrides = false;
			recalcState();
		}
	}

	@Override
	public @Nullable XivCombatant getCombatant(long id) {
		CombatantData cbt = combatantData.get(id);
		if (cbt == null) {
			WeakReference<XivCombatant> graveyardCbtRef = graveyard.get(id);
			if (graveyardCbtRef != null) {
				return graveyardCbtRef.get();
			}
			else {
				return null;
			}
		}
		else {
			return cbt.getComputed();
		}
	}

	private volatile boolean inCombat;

	@Override
	public boolean inCombat() {
		return inCombat;
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void inCombatChange(EventContext context, InCombatChangeEvent event) {
		this.inCombat = event.isInCombat();
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void zoneChange(EventContext context, ZoneChangeEvent event) {
		setZone(event.getZone());
		context.accept(new RefreshCombatantsRequest());
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void mapChange(EventContext context, MapChangeEvent event) {
		setMap(event.getMap());
		context.accept(new RefreshCombatantsRequest());
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void playerChange(EventContext context, RawPlayerChangeEvent event) {
		setPlayer(event.getPlayer());
		// After learning about the player, make sure we request combatant data
		context.accept(new RefreshCombatantsRequest());
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void combatantAdded(EventContext context, RawAddCombatantEvent event) {
		getOrCreateData(event.getEntity().getId()).setRawFromAct(event.getFullInfo());
		context.accept(new RefreshSpecificCombatantsRequest(Collections.singletonList(event.getEntity().getId())));
		recalcState();
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void combatantRemoved(EventContext context, RawRemoveCombatantEvent event) {
		CombatantData data = getData(event.getEntity().getId());
		if (data != null) {
			data.setRemoved(true);
		}
		context.accept(new RefreshCombatantsRequest());
		recalcState();
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void partyChange(EventContext context, PartyChangeEvent event) {
		setPartyList(event.getMembers());
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void partyForceOrderChange(EventContext context, PartyForceOrderChangeEvent event) {
		List<Long> newMembers = event.getMembers();
		if (newMembers == null) {
			partyListForceOrder = null;
		}
		else if (newMembers.isEmpty()) {
			partyListForceOrder = null;
		}
		else {
			partyListForceOrder = newMembers;
		}
		recalcState();
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void combatants(EventContext context, CombatantsUpdateRaw event) {
		if (event.isFullRefresh()) {
			setCombatants(event.getCombatantMaps());
		}
		else {
			setSpecificCombatants(event.getCombatantMaps());
		}
	}

	// For now, only caring about the primary player's online status so we can hide overlays in cutscenes
	@HandleEvents(order = Integer.MIN_VALUE)
	public void onlineStatus(EventContext context, RawOnlineStatusChanged event) {
		CombatantData data = getOrCreateData(event.getTargetId());
		OnlineStatus oldStatus = data.getStatus();
		OnlineStatus newStatus = OnlineStatus.forId(event.getRawStatusId());
		data.setStatus(newStatus);
		if (event.getTargetId() == getPlayerId()) {
			context.accept(new PrimaryPlayerOnlineStatusChangedEvent(oldStatus, newStatus));
		}
	}

	private final Map<Long, CombatantData> combatantData = new HashMap<>();
	// This lock is ONLY for adding/removing entries to the map. Individual values have their own locks.
	private final EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
	private Map<Long, XivCombatant> combatantCache = Collections.emptyMap();
	private final Map<Long, WeakReference<XivCombatant>> graveyard = new HashMap<>();

	private CombatantData getData(long cbtId) {
		// TODO: make something like
		try (LockAdapter ignored = lock.read()) {
			return combatantData.get(cbtId);
		}
	}

	private CombatantData getOrCreateData(long cbtId) {
		try (LockAdapter ignored = lock.write()) {
			return combatantData.computeIfAbsent(cbtId, CombatantData::new);
		}
	}

	//
	private final class CombatantData {
		// TODO: add party info?
		private final long id;
		private @Nullable RawXivCombatantInfo raw;
		private @Nullable Position posOverride;
		private @Nullable HitPoints hpOverride;
		private @Nullable ManaPoints mpOverride;
		private @Nullable XivCombatant fromOtherActLine;
		private @Nullable RawXivPartyInfo fromPartyInfo;
		private OnlineStatus status = OnlineStatus.UNKNOWN;
		private XivCombatant computed;
		private boolean fake;
		private volatile boolean dirty = true;
		private boolean removed;
		private XivCombatant owner;
		private long shieldPercent;

		private CombatantData(long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}

		public void setFake(boolean fake) {
			if (fake != this.fake) {
				this.fake = fake;
				dirty = true;
			}
		}

		public void setStatus(OnlineStatus status) {
			if (this.status != status) {
				this.status = status;
				dirty = true;
			}
		}

		public void setRawFromAct(@Nullable RawXivCombatantInfo raw) {
			if (this.raw == null) {
				setRaw(raw);
			}
		}

		public void setRaw(@Nullable RawXivCombatantInfo raw) {
			if (!Objects.equals(this.raw, raw) || posOverride != null) {
				this.raw = raw;
				this.posOverride = null;
				dirty = true;
			}
		}

		public void setFromPartyInfo(RawXivPartyInfo fromPartyInfo) {
			this.fromPartyInfo = fromPartyInfo;
			dirty = true;
		}

		public void setPosOverride(@Nullable Position posOverride) {
			if (!Objects.equals(this.posOverride, posOverride)) {
				this.posOverride = posOverride;
				dirty = true;
			}
		}

		public void setHpOverride(@Nullable HitPoints hpOverride) {
			if (!Objects.equals(this.hpOverride, hpOverride)) {
				this.hpOverride = hpOverride;
				dirty = true;
			}
		}

		public void setMpOverride(@Nullable ManaPoints mpOverride) {
			if (!Objects.equals(this.mpOverride, mpOverride)) {
				this.mpOverride = mpOverride;
				dirty = true;
			}
		}

		public void setFromOtherActLine(XivCombatant fromOtherActLine) {
			this.fromOtherActLine = fromOtherActLine;
			dirty = true;
		}

		public void setRemoved(boolean removed) {
			this.removed = removed;
			dirty = true;
		}

		public void setOwner(XivCombatant combatant) {
			this.owner = combatant;
			dirty = true;
		}

		public void setShieldPct(long shieldPct) {
			this.shieldPercent = shieldPct;
			dirty = true;
		}

		public OnlineStatus getStatus() {
			return status;
		}

		public boolean hasSufficientData() {
			return raw != null;
		}

		public boolean isRemoved() {
			return removed;
		}

		public boolean includeInList() {
			return !removed && hasSufficientData();
		}

		public boolean recomputeIfDirty() {
			if (dirty || computed == null) {
				recompute();
				return true;
			}
			return false;
		}

		private synchronized void recompute() {
			RawXivCombatantInfo raw = this.raw;
			// Each data element has a different "priority" for each field
			XivCombatant fromOther = fromOtherActLine;
			RawXivPartyInfo fromPartyInfo = this.fromPartyInfo;
			String name = raw != null ? raw.getName() : (fromOther != null ? fromOther.getName() : (fromPartyInfo != null ? fromPartyInfo.getName() : "???"));
			long jobId = raw != null ? raw.getJobId() : (fromPartyInfo != null ? fromPartyInfo.getJobId() : 0);
			XivWorld world = XivWorld.of();
			long rawType = raw != null ? raw.getRawType() : (id >= 0x4000_0000 ? 2 : 1);

			// HP prefers trusted ACT hp lines
			HitPoints hp = hpOverride != null ? hpOverride : raw != null ? raw.getHP() : null;
			ManaPoints mp = mpOverride != null ? mpOverride : raw != null ? raw.getMP() : null;
			Position pos = posOverride != null ? posOverride : raw != null ? raw.getPos() : fromOther != null ? fromOther.getPos() : null;

			XivCombatant computed;
			long bnpcId = raw != null ? raw.getBnpcId() : 0;
			long bnpcNameId = raw != null ? raw.getBnpcNameId() : 0;
			long partyType = raw != null ? raw.getPartyType() : 0;
			long level = raw != null ? raw.getLevel() : fromPartyInfo != null ? fromPartyInfo.getLevel() : 90;
			long ownerId = raw != null ? raw.getOwnerId() : 0;
			// TODO: changing primary player should dirty this
			boolean isPlayer = rawType == 1;
			long shieldAmount = hp != null ? shieldPercent * hp.max() / 100 : 0;
			if (isPlayer) {
				computed = new XivPlayerCharacter(
						id,
						name,
						Job.getById(jobId),
						world,
						id == getPlayerId(),
						rawType,
						hp,
						mp,
						pos,
						bnpcId,
						bnpcNameId,
						partyType,
						level,
						ownerId,
						shieldAmount);
			}
			else {
				computed = new XivCombatant(
						id,
						name,
						false,
						false,
						rawType,
						hp,
						mp,
						pos,
						bnpcId,
						bnpcNameId,
						partyType,
						level,
						ownerId,
						shieldAmount);
				if (bnpcId == 9020) {
					fake = true;
				}
				if (ownerId != 0) {
					computed.setParent(getCombatant(ownerId));
				}
			}
			// Earthly star workaround
			if (fake || computed.getbNpcId() == 7245) {
				fake = true;
				computed.setFake(true);
			}
			this.computed = computed;
			dirty = false;
		}

		// TODO: sync, or just update atomically?
		public synchronized XivCombatant getComputed() {
			recomputeIfDirty();
			return computed;
		}
	}
}
