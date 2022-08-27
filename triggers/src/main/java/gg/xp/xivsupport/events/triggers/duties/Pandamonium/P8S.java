package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "P8S", duty = KnownDuty.P8S)
public class P8S extends AutoChildEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P8S.class);
	private final ModifiableCallout<AbilityCastStart> genesisOfFlame = ModifiableCallout.durationBasedCall("Genesis Of Flame", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> sunforgePhoenix = ModifiableCallout.durationBasedCall("Sunforge Phoenix", "In");
	private final ModifiableCallout<AbilityCastStart> sunforgeSerpent = ModifiableCallout.durationBasedCall("Sunforge Serpent", "Out");
	private final ModifiableCallout<AbilityCastStart> reforgedReflectionQuadrupedal = ModifiableCallout.durationBasedCall("Reforged Reflection Quadrupedal", "Quadrapedal");
	private final ModifiableCallout<AbilityCastStart> reforgedReflectionSerpent = ModifiableCallout.durationBasedCall("Reforged Reflection Serpent", "Serpent");
	private final ModifiableCallout<AbilityCastStart> quadrupedalCrush = ModifiableCallout.durationBasedCall("Boss jumps to wall (Blazing Footfalls prep?)", "boss jump");
	private final ModifiableCallout<AbilityCastStart> trailblaze = ModifiableCallout.durationBasedCall("Blazing Footfalls Line KB", "line kb");
	private final ModifiableCallout<AbilityCastStart> quadrupedalImpact = ModifiableCallout.durationBasedCall("Blazing Footfalls AOE KB", "aoe kb");
	private final ModifiableCallout<AbilityCastStart> fourfoldFiresSafe = ModifiableCallout.durationBasedCall("Fourfold Fires Safe Spot", "{inter1} {inter2}"); //78F5

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P8S(XivState state) {
		this.state = state;
	}

	private final XivState state;

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0x7905)
				call = genesisOfFlame;
			if (id == 0x78EC)
				call = sunforgeSerpent;
			if (id == 0x78ED)
				call = sunforgePhoenix;
			if (id == 0x794B)
				call = reforgedReflectionQuadrupedal;
			if (id == 0x794C)
				call = reforgedReflectionSerpent;
			if (id == 0x7904)
				call = quadrupedalCrush;
			if (id == 0x790D)
				call = trailblaze;
			if (id == 0x7103)
				call = quadrupedalImpact;
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cthonicVent = new SequentialTrigger<>(
			10_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x78F5),
			(e1, s) -> {
				List<AbilityCastStart> cthonicCasts = new ArrayList<>(s.waitEvents(1, AbilityCastStart.class, event -> event.abilityIdMatches(0x78F5)));
				cthonicCasts.add((AbilityCastStart) e1);
				List<XivCombatant> suneaters = new ArrayList<>();
				log.info("CthonicVent: Got suneater casts");
				s.refreshCombatants(100);
				log.info("CthonicVent: done with delay");
				for(AbilityCastStart acs : cthonicCasts) {
					suneaters.add(acs.getSource());
				}
				log.info("CthonicVent: done finding positions, finding safe spots");
				if(suneaters.size() != 2) {
					log.error("Invalid number of suneaters found! Data: {}", cthonicCasts);
					return;
				}
				String inter1 = arenaPos.forCombatant(suneaters.get(0)).getFriendlyName();
				String inter2 = arenaPos.forCombatant(suneaters.get(1)).getFriendlyName();

				Map<String, Object> args = Map.of("inter1", inter1, "inter2", inter2);
				s.accept(fourfoldFiresSafe.getModified(cthonicCasts.get(0), args));
			}
	);
}
