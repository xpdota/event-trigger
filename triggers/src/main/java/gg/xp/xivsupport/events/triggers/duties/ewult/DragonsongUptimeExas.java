package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.OverridesCalloutGroupEnabledSetting;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumListSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@CalloutRepo(name = "South Uptime Exas", duty = KnownDuty.Dragonsong)
public class DragonsongUptimeExas extends AutoChildEventHandler implements FilteredEventHandler, OverridesCalloutGroupEnabledSetting {

	// Thank you to mnk_lounge for dragging my small brain through this logic

	private static final Logger log = LoggerFactory.getLogger(DragonsongUptimeExas.class);
	private final XivState state;
	private final BooleanSetting enabled;
	private final EnumListSetting<UptimeExaflareMovement> priority;

	private final ModifiableCallout<AbilityCastStart> southPlant = new ModifiableCallout<>("South Plant", "South Plant", 8_000);
	private final ModifiableCallout<AbilityCastStart> southNorth = new ModifiableCallout<>("South Then North (Wide)", "South Then North", 8_000);
	private final ModifiableCallout<AbilityCastStart> southNorthNarrow = new ModifiableCallout<>("South Then North (Narrow)", "South Then North", 8_000);
	private final ModifiableCallout<AbilityCastStart> southWest = new ModifiableCallout<>("South Then West", "South Then West", 8_000);
	private final ModifiableCallout<AbilityCastStart> southEast = new ModifiableCallout<>("South Then East", "South Then East", 8_000);
	private final ModifiableCallout<AbilityCastStart> northwestPlant = new ModifiableCallout<>("Northwest Plant", "Northwest Plant", 8_000);
	private final ModifiableCallout<AbilityCastStart> northeastPlant = new ModifiableCallout<>("Northeast Plant", "Northeast Plant", 8_000);
	private final ModifiableCallout<AbilityCastStart> badPattern = new ModifiableCallout<>("Bad Pattern", "Downtime Pattern", 8_000);
	private final ModifiableCallout<AbilityCastStart> error = new ModifiableCallout<>("Trigger Error", "Downtime Pattern", 8_000);

	// TODO: make tests for this
	public DragonsongUptimeExas(XivState state, PersistenceProvider pers) {
		this.state = state;
		this.enabled = new BooleanSetting(pers, "triggers.dragonsong.south-uptime-exas.enabled", false);
		this.priority = new EnumListSetting<>(UptimeExaflareMovement.class, pers, "triggers.dragonsong.south-uptime-exas.prio", EnumListSetting.BadKeyBehavior.OMIT, UptimeExaflareMovement.defaultOrder());
	}

	public XivState getState() {
		return state;
	}

	public EnumListSetting<UptimeExaflareMovement> getPriority() {
		return priority;
	}

	@Override
	public boolean enabled(EventContext context) {
		return enabled.get() && state.zoneIs(0x3C8);
	}

	// TODO: There's more info about how uptime exaflares work now
	/*
		The "downtime" pattern is unnecessary, because if NW and NE are intercard, and S is card, then NW/NE aren't
		getting hit by anything. Thus, you can plant NW or NE.

		There's also a second strat, where you can just *always* plant. It appears that you cannot actually get a
		pattern without a plantable exaflare.
	 */

	// Check if any of an exa's three directions are equal to some given direction.
	// i.e. is 'direction' going to get hit by 'exaflarefacing' (front, left or right)
	private static boolean willExaHitDirection(ArenaSector direction, ArenaSector exaflareFacing) {
		return exaflareFacing == direction || exaflareFacing.plusQuads(1) == direction || exaflareFacing.plusQuads(-1) == direction;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> uptimeExas = new SequentialTrigger<>(
			10_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(28059, 28060),
			(e1, s) -> {
				log.info("Uptime exas: start");
				// The real cast is not necessarily always the first or last, so we need to combine all four and go from there
				List<AbilityCastStart> exaCasts = new ArrayList<>(s.waitEvents(3, AbilityCastStart.class, event -> event.abilityIdMatches(28059, 28060)));
				exaCasts.add((AbilityCastStart) e1);
				AbilityCastStart realThordanCast = exaCasts.stream().filter(acs -> acs.getSource().getbNpcId() == 12616).findFirst().orElseThrow(() -> new RuntimeException("Uptime exas: couldn't find real Thordan cast!"));
				exaCasts.remove(realThordanCast);
				log.info("Uptime exas: got casts");
				s.refreshCombatants(100);
				log.info("Uptime exas: done with delay");
				XivCombatant thordan = getState().getLatestCombatantData(realThordanCast.getSource());
				Position thordanPos = thordan.getPos();
				List<Position> normalizedExaPositions = exaCasts.stream().map(HasSourceEntity::getSource)
						.map(getState()::getLatestCombatantData)
						.filter(Objects::nonNull)
						.map(actor -> actor.getPos().normalizedTo(thordanPos))
						.toList();
				if (normalizedExaPositions.size() != 3) {
					log.error("Missing an exaflare actor! Data: {} - {}", exaCasts, normalizedExaPositions);
					s.updateCall(error.getModified(realThordanCast));
					return;
				}
				log.info("Uptime exas: computing");
				ArenaPos apos = new ArenaPos(0, 0, 3, 3);
				Position southExaPos = normalizedExaPositions.stream().filter(pos -> apos.forPosition(pos) == ArenaSector.SOUTH).findFirst().orElseThrow(() -> new RuntimeException("Could not find south exaflare"));
				Position westExaPos = normalizedExaPositions.stream().filter(pos -> apos.forPosition(pos) == ArenaSector.NORTHWEST).findFirst().orElseThrow(() -> new RuntimeException("Could not find northwest exaflare"));
				Position eastExaPos = normalizedExaPositions.stream().filter(pos -> apos.forPosition(pos) == ArenaSector.NORTHEAST).findFirst().orElseThrow(() -> new RuntimeException("Could not find northeast exaflare"));
				ArenaSector southFacing = ArenaPos.combatantFacing(southExaPos.getHeading());
				ArenaSector westFacing = ArenaPos.combatantFacing(westExaPos.getHeading());
				ArenaSector eastFacing = ArenaPos.combatantFacing(eastExaPos.getHeading());

				log.info("Uptime exaflares: S {}, NW {}, NE {}", southFacing, westFacing, eastFacing);
				List<UptimeExaflareMovement> safeSpots = getSafeMovements(southFacing, westFacing, eastFacing);
				final ModifiableCallout<AbilityCastStart> call;
				if (safeSpots.isEmpty()) {
					// Should never happen, just in case
					call = badPattern;
				}
				else {
					call = switch (safeSpots.get(0)) {
						case SOUTH_PLANT -> southPlant;
						case SOUTH_NORTH -> southNorth;
						case SOUTH_WEST -> southWest;
						case SOUTH_EAST -> southEast;
						case NORTHWEST_PLANT -> northwestPlant;
						case NORTHEAST_PLANT -> northeastPlant;
						case SOUTH_NORTH_NARROW -> southNorthNarrow;
						case DOWNTIME -> badPattern;
					};
				}
				s.updateCall(call.getModified(realThordanCast));
			}
	);

	// TODO: unit test
	final List<UptimeExaflareMovement> getSafeMovements(ArenaSector southFacing, ArenaSector westFacing, ArenaSector eastFacing) {
		List<UptimeExaflareMovement> safeSpots = new ArrayList<>(getPriority().get());
		// Remove things that S will hit
		if (willExaHitDirection(ArenaSector.NORTHWEST, southFacing)) {
			safeSpots.remove(UptimeExaflareMovement.NORTHWEST_PLANT);
		}
		if (willExaHitDirection(ArenaSector.NORTHEAST, southFacing)) {
			safeSpots.remove(UptimeExaflareMovement.NORTHEAST_PLANT);
		}
		if (willExaHitDirection(ArenaSector.NORTH, southFacing)) {
			safeSpots.remove(UptimeExaflareMovement.SOUTH_NORTH);
			safeSpots.remove(UptimeExaflareMovement.SOUTH_NORTH_NARROW);
		}
		// Remove things that NW will hit
		if (willExaHitDirection(ArenaSector.EAST, westFacing)) {
			safeSpots.remove(UptimeExaflareMovement.NORTHEAST_PLANT);
			safeSpots.remove(UptimeExaflareMovement.SOUTH_NORTH);
		}
		if (willExaHitDirection(ArenaSector.SOUTH, westFacing)) {
			safeSpots.remove(UptimeExaflareMovement.SOUTH_WEST);
		}
		if (willExaHitDirection(ArenaSector.SOUTHEAST, westFacing)) {
			safeSpots.remove(UptimeExaflareMovement.SOUTH_PLANT);
			safeSpots.remove(UptimeExaflareMovement.SOUTH_EAST);
		}
		// Remove things that NE will hit
		if (willExaHitDirection(ArenaSector.WEST, eastFacing)) {
			safeSpots.remove(UptimeExaflareMovement.NORTHWEST_PLANT);
			safeSpots.remove(UptimeExaflareMovement.SOUTH_NORTH);
		}
		if (willExaHitDirection(ArenaSector.SOUTH, eastFacing)) {
			safeSpots.remove(UptimeExaflareMovement.SOUTH_EAST);
		}
		if (willExaHitDirection(ArenaSector.SOUTHWEST, eastFacing)) {
			safeSpots.remove(UptimeExaflareMovement.SOUTH_PLANT);
			safeSpots.remove(UptimeExaflareMovement.SOUTH_WEST);
		}

		log.info("Computed safe strats: {}", safeSpots);
		return safeSpots;

	}

	@Override
	public BooleanSetting getCalloutGroupEnabledSetting() {
		return enabled;
	}
}
