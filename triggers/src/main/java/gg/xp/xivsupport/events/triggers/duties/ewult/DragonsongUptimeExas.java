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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CalloutRepo(name = "South Uptime Exas", duty = KnownDuty.Dragonsong)
public class DragonsongUptimeExas extends AutoChildEventHandler implements FilteredEventHandler, OverridesCalloutGroupEnabledSetting {

	// Thank you to mnk_lounge for dragging my small brain through this logic

	private static final Logger log = LoggerFactory.getLogger(DragonsongUptimeExas.class);
	private final XivState state;
	private final BooleanSetting enabled;

	private final ModifiableCallout<AbilityCastStart> southPlant = new ModifiableCallout<>("South Plant", "South Plant", 8_000);
	private final ModifiableCallout<AbilityCastStart> southNorth = new ModifiableCallout<>("South Then North", "South Then North", 8_000);
	private final ModifiableCallout<AbilityCastStart> southWest = new ModifiableCallout<>("South Then West", "South Then West", 8_000);
	private final ModifiableCallout<AbilityCastStart> southEast = new ModifiableCallout<>("South Then East", "South Then East", 8_000);
	private final ModifiableCallout<AbilityCastStart> badPattern = new ModifiableCallout<>("Bad Pattern", "Downtime Pattern", 8_000);

	// TODO: make tests for this
	public DragonsongUptimeExas(XivState state, PersistenceProvider pers) {
		this.state = state;
		this.enabled = new BooleanSetting(pers, "triggers.dragonsong.south-uptime-exas.enabled", false);
	}

	public XivState getState() {
		return state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return enabled.get() && state.zoneIs(0x3C8);
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

				boolean sUnsafeFromW = westFacing == ArenaSector.NORTHEAST || westFacing == ArenaSector.SOUTHEAST || westFacing == ArenaSector.SOUTHWEST;
				boolean sUnsafeFromE = eastFacing == ArenaSector.NORTHWEST || eastFacing == ArenaSector.SOUTHEAST || eastFacing == ArenaSector.SOUTHWEST;
				boolean nUnsafeFromS = southFacing == ArenaSector.NORTH || southFacing == ArenaSector.WEST || southFacing == ArenaSector.EAST;

				ModifiableCallout<AbilityCastStart> call;
				if (sUnsafeFromW && sUnsafeFromE) {
					if (nUnsafeFromS) {
						// bad pattern, can't uptime with this strat
						call = badPattern;
					}
					else {
						// south then north
						call = southNorth;
					}
				}
				else {
					if (!sUnsafeFromW && !sUnsafeFromE) {
						// Neither NW nor NE will hit S, so just dodge in and plant.
						call = southPlant;
					}
					else {
						if (sUnsafeFromW) {
							// South then slightly west
							call = southWest;
						}
						else {
							// South then slightly east
							call = southEast;
						}
					}
				}
				s.accept(call.getModified(realThordanCast));

				// If card/intercard - dodge into south exa, then to the side of inter
				// If card/card - sit in south
				// If ic/ic - dodge into south exa, then to north side of hitbox
			}
	);

	@Override
	public BooleanSetting getCalloutGroupEnabledSetting() {
		return enabled;
	}
}
