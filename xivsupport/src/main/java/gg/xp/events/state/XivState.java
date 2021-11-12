package gg.xp.events.state;

import gg.xp.context.SubState;
import gg.xp.events.EventMaster;
import gg.xp.events.actlines.data.Job;
import gg.xp.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.events.models.XivCombatant;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivWorld;
import gg.xp.events.models.XivZone;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class XivState implements SubState {

	private static final Logger log = LoggerFactory.getLogger(XivState.class);
	private final EventMaster master;

	private XivZone zone;
	// EARLY player info before we have combatant data
	private XivEntity playerPartial;
	// FULL player info after we get the stuff we need
	private XivPlayerCharacter player;
	private @NotNull List<RawXivPartyInfo> partyListRaw = Collections.emptyList();
	private final @NotNull List<XivPlayerCharacter> partyListProcessed = new ArrayList<>();
	private @NotNull Map<Long, RawXivCombatantInfo> combatantsRaw = Collections.emptyMap();
	private final @NotNull Map<Long, XivCombatant> combatantsProcessed = new HashMap<>();

	@SuppressWarnings("unused")
	@Deprecated
	XivState() {
		// TODO: this is still needed for tests
		log.warn("Using old XivState ctor");
		this.master = null;
	}

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

	public void setZone(XivZone zone) {
		log.info("Zone changed to {}", zone);
		this.zone = zone;
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
						new XivWorld(),
						// TODO
						0,
						true);
			}
		}
		combatantsProcessed.clear();
		combatantsRaw.forEach((unused, combatant) -> {
			long id = combatant.getId();
			int jobId = combatant.getJobId();
			XivCombatant value;
			if (combatant.getType() == 1) {
				value = new XivPlayerCharacter(
						id,
						combatant.getName(),
						Job.getById(jobId),
						// TODO
						new XivWorld(),
						// TODO
						0,
						false);
			}
			else {
				value = new XivCombatant(
						id,
						combatant.getName(),
						false,
						false
				);
			}
			combatantsProcessed.put(id, value);
		});
		// lazy but works
		combatantsProcessed.put(playerId, player);
		partyListProcessed.clear();
		partyListRaw.forEach(rawPartyMember -> {
			long id = rawPartyMember.getId();
			XivCombatant fullCombatant = combatantsProcessed.get(id);
			if (fullCombatant instanceof XivPlayerCharacter) {
				partyListProcessed.add((XivPlayerCharacter) fullCombatant);
			}
			else {
				partyListProcessed.add(new XivPlayerCharacter(
						rawPartyMember.getId(),
						rawPartyMember.getName(),
						Job.getById(rawPartyMember.getJobId()),
						new XivWorld(rawPartyMember.getWorldId()),
						0, // TODO
						false));
			}
		});
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
		log.info("Recalculated state, player is {}, party is {}", player, partyListProcessed);
		// TODO: improve this
		if (master != null) {
			master.getQueue().push(new XivStateRecalculatedEvent());
		}
	}

	public boolean zoneIs(long zoneId) {
		XivZone zone = getZone();
		return zone != null && zone.getId() == zoneId;
	}

	public void setCombatants(List<RawXivCombatantInfo> combatants) {
		this.combatantsRaw = new HashMap<>(combatants.size());
		combatants.forEach(combatant -> {
			long id = combatant.getId();
			// Fake/environment actors
			if (id == 0xE0000000L) {
				return;
			}
			RawXivCombatantInfo old = this.combatantsRaw.put(id, combatant);
			if (old != null) {
				log.warn("Duplicate combatant data for id {}: old ({}) vs new ({})", id, old, combatant);
			}
		});
		log.info("Received info on {} combatants", combatants.size());
		recalcState();
	}

	public Map<Long, XivCombatant> getCombatants() {
		return Collections.unmodifiableMap(combatantsProcessed);
	}
}
