package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CalloutRepo(name = "P8N", duty = KnownDuty.P8N)
public class P8N extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P8N.class);
	private final ModifiableCallout<AbilityCastStart> genesisOfFlame = ModifiableCallout.durationBasedCall("Genesis Of Flame", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> rearingRampage = ModifiableCallout.durationBasedCall("Rearing Rampage", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> ektothermos = ModifiableCallout.durationBasedCall("Ektothermos", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> sunforgePhoenix = ModifiableCallout.durationBasedCall("Sunforge Phoenix", "In");
	private final ModifiableCallout<AbilityCastStart> sunforgeSerpent = ModifiableCallout.durationBasedCall("Sunforge Serpent", "Out");
	private final ModifiableCallout<AbilityCastStart> reforgedReflectionQuadruped = ModifiableCallout.durationBasedCall("Reforged Reflection Quadruped", "Quadraped");
	private final ModifiableCallout<AbilityCastStart> reforgedReflectionSerpent = ModifiableCallout.durationBasedCall("Reforged Reflection Serpent", "Serpent");
	private final ModifiableCallout<AbilityCastStart> fourfoldFiresSafe = ModifiableCallout.durationBasedCall("Fourfold Fires Safe Spot", "{safe}");
	private final ModifiableCallout<AbilityCastStart> flameviper = ModifiableCallout.durationBasedCall("Flameviper", "Tankbuster on {event.target}");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P8N(XivState state) {
		this.state = state;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x43F);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0x7905)
				call = genesisOfFlame;
			else if (id == 0x78EC)
				call = sunforgeSerpent;
			else if (id == 0x78ED)
				call = sunforgePhoenix;
			else if (id == 0x794B)
				call = reforgedReflectionQuadruped;
			else if (id == 0x794C)
				call = reforgedReflectionSerpent;
			else if (id == 0x7908)
				call = flameviper;
			else if (id == 0x79AB)
				call = rearingRampage;
			else if (id == 0x78FE)
				call = ektothermos;
			else
				return;

			context.accept(call.getModified(event));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cthonicVent = new SequentialTrigger<>(
			10_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x78F5, 0x794D, 0x78F6),
			(e1, s) -> {
				List<AbilityCastStart> cthonicCasts = new ArrayList<>(s.waitEvents(1, AbilityCastStart.class, event -> event.abilityIdMatches(0x78F5, 0x794D, 0x78F6)));
				cthonicCasts.add((AbilityCastStart) e1);
				List<XivCombatant> suneaters = new ArrayList<>();
				log.info("CthonicVent: Got suneater casts");
				s.waitMs(100);
				s.refreshCombatants(100);
				log.info("CthonicVent: done with delay");
				for(AbilityCastStart acs : cthonicCasts) {
					suneaters.add(this.getState().getLatestCombatantData(acs.getSource()));
				}
				log.info("CthonicVent: done finding positions, finding safe spots");
				if(suneaters.size() != 2) {
					log.error("Invalid number of suneaters found! Data: {}", cthonicCasts);
					return;
				}
				log.info("CthonicVent: found suneaters 1:{}, 2:{}", arenaPos.forCombatant(suneaters.get(0)).getFriendlyName(), arenaPos.forCombatant(suneaters.get(1)).getFriendlyName());
				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.quadrants);
				safe.remove(arenaPos.forCombatant(suneaters.get(0)));
				safe.remove(arenaPos.forCombatant(suneaters.get(1)));

				Map<String, Object> args = Map.of("safe", safe);
				s.accept(fourfoldFiresSafe.getModified(cthonicCasts.get(0), args));
			}
	);

	//TODO: blazing footfalls sequential. 7900 boss cast, 790b line kb precast, 790c circle precast. kb precasts happen before 7900 finishes casting.
}
