package gg.xp.xivsupport.events.state;

import gg.xp.reevent.context.SubState;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.XivMap;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.models.XivZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XivState implements SubState {

	private static final Logger log = LoggerFactory.getLogger(XivState.class);
	private final EventMaster master;

	private XivZone zone;
	private XivMap map = XivMap.UNKNOWN;
	// EARLY player info before we have combatant data
	private XivEntity playerPartial;
	// FULL player info after we get the stuff we need
	private XivPlayerCharacter player;
	// TODO: see if same-world parties with out-of-zone players would break this
	private volatile @NotNull List<RawXivPartyInfo> partyListRaw = Collections.emptyList();
	private volatile @NotNull List<XivPlayerCharacter> partyListProcessed = Collections.emptyList();
	private volatile @NotNull Map<Long, RawXivCombatantInfo> combatantsRaw = Collections.emptyMap();
	private volatile @NotNull Map<Long, XivCombatant> combatantsProcessed = Collections.emptyMap();
	private final Map<Long, HitPoints> hpOverrides = new HashMap<>();
	private final Map<Long, Position> posOverrides = new HashMap<>();
	private volatile Map<Long, SoftReference<XivCombatant>> graveyard = new HashMap<>();
	private boolean isActImport;


	private Job previousPlayerJob;
//	@SuppressWarnings("unused")
//	@Deprecated
//	XivState() {
//		// TODO: this is still needed for tests
//		log.warn("Using old XivState ctor");
//		this.master = null;
//	}

	public XivState(EventMaster master) {
		this.master = master;
	}

	// Note: can be null until we have all the required data, but this should only happen very early on in init
	public XivPlayerCharacter getPlayer() {
		return player;
	}

	public void setPlayer(XivEntity player) {
		log.info("Player changed to {}", player);
		this.playerPartial = player;
		recalcState();
	}

	// Note: can be null until we've seen a 01-line
	public XivZone getZone() {
		return zone;
	}

	public XivMap getMap() {
		return map;
	}

	public void setZone(XivZone zone) {
		log.info("Zone changed to {}", zone);
		this.zone = zone;
	}

	public void setMap(XivMap map) {
		log.info("Map changed to {}", map);
		this.map = map;
	}

	public void setPartyList(List<RawXivPartyInfo> partyList) {
		this.partyListRaw = new ArrayList<>(partyList);
		recalcState();
		log.info("Party list changed to {}", this.partyListProcessed.stream().map(XivEntity::getName).collect(Collectors.joining(", ")));
	}

	public List<XivPlayerCharacter> getPartyList() {
		return new ArrayList<>(partyListProcessed);
	}


	// TODO: concurrency issues?
	// TODO: emit events after state is recalculated reflecting actual changes
	// Probably requires proper equals/hashcode on everything so we can actually compare them
	private void recalcState() {
		// *Shouldn't* happen, but no reason to fail when we'll get the data soon anyway
		if (playerPartial == null) {
			return;
		}
		long playerId = playerPartial.getId();
		{
			RawXivCombatantInfo playerCombatantInfo = combatantsRaw.get(playerId);
			if (playerCombatantInfo != null) {
				player = new XivPlayerCharacter(
						playerCombatantInfo.getId(),
						playerCombatantInfo.getName(),
						Job.getById(playerCombatantInfo.getJobId()),
						// TODO
						XivWorld.of(),
						// TODO
						true,
						playerCombatantInfo.getRawType(),
						hpOverrides.getOrDefault(playerCombatantInfo.getId(), playerCombatantInfo.getHP()),
						playerCombatantInfo.getMP(),
						posOverrides.getOrDefault(playerCombatantInfo.getId(), playerCombatantInfo.getPos()),
						playerCombatantInfo.getBnpcId(),
						playerCombatantInfo.getBnpcNameId(),
						playerCombatantInfo.getPartyType(),
						playerCombatantInfo.getLevel(),
						playerCombatantInfo.getOwnerId());
			}
		}
		Map<Long, XivCombatant> combatantsProcessed = new HashMap<>(combatantsRaw.size());
		Map<Long, List<XivCombatant>> combatantsByNpcName = new HashMap<>();
		combatantsRaw.forEach((unused, combatant) -> {
			long id = combatant.getId();
			long jobId = combatant.getJobId();
			long rawType = combatant.getRawType();
			XivCombatant value;
			if (rawType == 1) {
				value = new XivPlayerCharacter(
						id,
						combatant.getName(),
						Job.getById(jobId),
						// TODO
						XivWorld.of(),
						// TODO
						false,
						combatant.getRawType(),
						hpOverrides.getOrDefault(combatant.getId(), combatant.getHP()),
						combatant.getMP(),
						posOverrides.getOrDefault(combatant.getId(), combatant.getPos()),
						combatant.getBnpcId(),
						combatant.getBnpcNameId(),
						combatant.getPartyType(),
						combatant.getLevel(),
						combatant.getOwnerId());
			}
			else {
				value = new XivCombatant(
						id,
						combatant.getName(),
						false,
						false,
						combatant.getRawType(),
						hpOverrides.getOrDefault(combatant.getId(), combatant.getHP()),
						combatant.getMP(),
						posOverrides.getOrDefault(combatant.getId(), combatant.getPos()),
						combatant.getBnpcId(),
						combatant.getBnpcNameId(),
						combatant.getPartyType(),
						combatant.getLevel(),
						combatant.getOwnerId());
				combatantsByNpcName.computeIfAbsent(value.getbNpcNameId(), (ignore) -> new ArrayList<>()).add(value);
			}
			combatantsProcessed.put(id, value);
		});
		combatantsProcessed.values().forEach(c -> {
			long ownerId = c.getOwnerId();
			if (ownerId != 0) {
				c.setParent(combatantsProcessed.get(ownerId));
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
			values.sort(Comparator.<XivCombatant, Long>comparing(npc -> npc.getHp() != null ? npc.getHp().getMax() : 0).reversed());
			XivCombatant primaryCombatant = values.get(0);
			if (primaryCombatant.getHp() == null) {
				return;
			}
			List<XivCombatant> potentialFakes = new ArrayList<>();
			for (XivCombatant otherCombatant : values.subList(1, values.size())) {
				if (otherCombatant.getHp() == null) {
					continue;
				}
				if (otherCombatant.getHp().getMax() < primaryCombatant.getHp().getMax()
						&& otherCombatant.getbNpcId() != primaryCombatant.getbNpcId()) {
					potentialFakes.add(otherCombatant);
				}
			}
			if (potentialFakes.size() >= 2) {
				XivCombatant firstPossibleFake = potentialFakes.get(0);
				boolean allMatch = potentialFakes.subList(1, potentialFakes.size()).stream().allMatch(p -> p.getPos() != null && p.getPos().equals(firstPossibleFake.getPos()));
				if (allMatch) {
					potentialFakes.forEach(fake -> {
						fake.setFake(true);
						// Also use Parent field to link to real NPC if it doesn't already have a real owner
						if (fake.getParent() == null) {
							fake.setParent(primaryCombatant);
						}
					});
				}
			}
		});
		// lazy but works
		if (player != null) {
			combatantsProcessed.put(playerId, player);
		}
		combatantsProcessed.values().forEach(c -> {
			long ownerId = c.getOwnerId();
			if (ownerId != 0) {
				c.setParent(combatantsProcessed.get(ownerId));
			}
		});
		// TODO: just doing a simple diff of this would be a great way to synthesize
		// add/remove combatant events
		this.combatantsProcessed = combatantsProcessed;
		List<XivPlayerCharacter> partyListProcessed = new ArrayList<>(partyListRaw.size());
		List<RawXivPartyInfo> partyMembersNotInCombatants = new ArrayList<>();
		partyListRaw.forEach(rawPartyMember -> {
			if (!rawPartyMember.isInParty()) {
				// Member of different alliance, ignore
				// TODO: is there value in supporting alliance stuff?
				return;
			}
			long id = rawPartyMember.getId();
			XivCombatant fullCombatant = combatantsProcessed.get(id);
			if (fullCombatant instanceof XivPlayerCharacter) {
				partyListProcessed.add((XivPlayerCharacter) fullCombatant);
			}
			else {
				partyMembersNotInCombatants.add(rawPartyMember);
				partyListProcessed.add(new XivPlayerCharacter(
						rawPartyMember.getId(),
						rawPartyMember.getName(),
						Job.getById(rawPartyMember.getJobId()),
						XivWorld.createXivWorld(rawPartyMember.getWorldId()),
						false,
						0,
						null,
						null,
						null,
						0,
						0,
						0,
						0,
						0
				));
			}
		});
		if (!partyMembersNotInCombatants.isEmpty()) {
			log.debug("Party member(s) not in combatants: {}", partyMembersNotInCombatants.stream()
					.map(raw -> raw.getName() != null && !raw.getName().isEmpty() ? raw.getName() : ("0x" + Long.toString(raw.getId(), 16)))
					.collect(Collectors.joining(", ")));
		}
		partyListProcessed.sort(Comparator.comparing(p -> {
			if (player != null && player.getId() == p.getId()) {
				// Always sort main player first
				return -1;
			}
			else {
				// TODO: customizable party sorting
				return p.getJob().partySortOrder();
			}
		}));
		if (partyListProcessed.isEmpty() && player != null) {
			this.partyListProcessed = List.of(player);
		}
		else {
			this.partyListProcessed = partyListProcessed;
		}
		log.trace("Recalculated state, player is {}, party is {}", player, partyListProcessed);
		// TODO: improve this
		if (master != null) {
			master.getQueue().push(new XivStateRecalculatedEvent());
			if (player != null) {
				Job newJob = player.getJob();
				if (previousPlayerJob != null && previousPlayerJob != newJob) {
					master.getQueue().push(new PlayerChangedJobEvent(previousPlayerJob, newJob));
				}
				previousPlayerJob = newJob;
			}
		}
		combatantsProcessed.forEach((id, cbt) -> graveyard.putIfAbsent(id, new SoftReference<>(cbt)));
	}

	public boolean zoneIs(long zoneId) {
		XivZone zone = getZone();
		return zone != null && zone.getId() == zoneId;
	}

	public void setCombatants(List<RawXivCombatantInfo> combatants) {
		Map<Long, RawXivCombatantInfo> combatantsRaw = new HashMap<>(combatants.size());
		combatants.forEach(combatant -> {
			long id = combatant.getId();
			// Fake/environment actors
			// TODO: should we still be doing this?
			if (id == 0xE0000000L) {
				return;
			}
			RawXivCombatantInfo old = combatantsRaw.put(id, combatant);
			if (old != null) {
				log.warn("Duplicate combatant data for id {}: old ({}) vs new ({})", id, old, combatant);
			}
		});
		log.trace("Received info on {} combatants", combatants.size());
		this.combatantsRaw = combatantsRaw;
		hpOverrides.clear();
		posOverrides.clear();
		recalcState();
	}

	public void setSpecificCombatants(List<RawXivCombatantInfo> combatants) {
		Map<Long, RawXivCombatantInfo> combatantsRaw = new HashMap<>(this.combatantsRaw.size() + combatants.size());
		combatantsRaw.putAll(this.combatantsRaw);
		combatants.forEach(combatant -> {
			long id = combatant.getId();
			hpOverrides.remove(id);
			// Fake/environment actors
			// TODO: should we still be doing this?
			if (id == 0xE0000000L) {
				return;
			}
			combatantsRaw.put(id, combatant);
		});
		log.trace("Received info on {} combatants", combatants.size());
		this.combatantsRaw = combatantsRaw;
		recalcState();
	}

	public void removeSpecificCombatant(long idToRemove) {
		combatantsRaw.remove(idToRemove);
		XivCombatant cbt = combatantsProcessed.get(idToRemove);
		if (cbt != null) {
			graveyard.put(cbt.getId(), new SoftReference<>(cbt));
		}
	}

	public Map<Long, XivCombatant> getCombatants() {
		return Collections.unmodifiableMap(combatantsProcessed);
	}

	// TODO: does this still need to be a copy?
	public List<XivCombatant> getCombatantsListCopy() {
		return new ArrayList<>(combatantsProcessed.values());
	}

	public int getPartySlotOf(XivEntity entity) {
		List<XivPlayerCharacter> partyList = getPartyList();
		return IntStream.range(0, partyList.size())
				.filter(i -> partyList.get(i).getId() == entity.getId())
				.findFirst()
				.orElse(-1);
	}

	private boolean dirtyOverrides;

	public void provideCombatantHP(XivCombatant target, @NotNull HitPoints hitPoints) {
		HitPoints oldOverride = hpOverrides.put(target.getId(), hitPoints);
		// Only trigger refresh if something actually changed, or there is already a pending refresh
		if (dirtyOverrides) {
			return;
		}
		// The only time we want to completely ignore an update is if the new override has no real effect.
		// i.e. it is the same as the current HP, and either there is no previous override, or the previous override is
		// the same as the current.

		// Old override
		if (oldOverride == null || oldOverride.equals(hitPoints)) {
			// If there was no previous override, and the HP from WS == HP from the event, ignore
			XivCombatant cbt = combatantsProcessed.get(target.getId());
			if (cbt != null) {
				// new override = existing WS hp
				HitPoints existingHp = cbt.getHp();
				if (hitPoints.equals(existingHp)) {
					return;
				}
			}
		}
		dirtyOverrides = true;
	}

	public void provideCombatantPos(XivCombatant target, Position newPos) {
		Position oldOverride = posOverrides.put(target.getId(), newPos);
		// Only trigger refresh if something actually changed, or there is already a pending refresh
		if (dirtyOverrides) {
			return;
		}
		if (oldOverride == null || oldOverride.equals(newPos)) {
			// If there was no previous override, and the HP from WS == HP from the event, ignore
			XivCombatant cbt = combatantsProcessed.get(target.getId());
			if (oldOverride == null && cbt != null) {
				Position existingPos = cbt.getPos();
				if (newPos.equals(existingPos)) {
					return;
				}
			}
			dirtyOverrides = true;
		}
	}

	public void flushProvidedValues() {
		if (dirtyOverrides) {
			dirtyOverrides = false;
			recalcState();
		}
	}

	public @Nullable XivCombatant getDeadCombatant(long id) {
		SoftReference<XivCombatant> ref = graveyard.get(id);
		if (ref == null) {
			return null;
		}
		return ref.get();
	}

	public boolean isActImport() {
		return isActImport;
	}

	public void setActImport(boolean actImport) {
		this.isActImport = actImport;
	}
}
