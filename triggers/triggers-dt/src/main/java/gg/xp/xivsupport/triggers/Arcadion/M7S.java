package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

@CalloutRepo(name = "M7S", duty = KnownDuty.M7S)
public class M7S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M7S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M7S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M7S);
	}

	@NpcCastCallout(0xA55B)
	private final ModifiableCallout<AbilityCastStart> brutalImpact = ModifiableCallout.durationBasedCall("Brutal Impact", "Multi Hit Raidwide");

	@NpcCastCallout(0xA560)
	private final ModifiableCallout<AbilityCastStart> smashThere = ModifiableCallout.durationBasedCall("Smash There", "Tanks Out, Party In");
	@NpcCastCallout(0xA55F)
	private final ModifiableCallout<AbilityCastStart> smashHere = ModifiableCallout.durationBasedCall("Smash There", "Tanks In, Party Out");

	private static final ArenaPos pollenPos = new ArenaPos(100, 100, 8, 8);

	private final ModifiableCallout<AbilityCastStart> pollenSafe = ModifiableCallout.<AbilityCastStart>durationBasedCall("Pollen Safe Spots", "{safeCorners} Corners Safe")
			.extendedDescription("""
					To use inner safe spots instead of corners, use {safeInner} instead of {safeCorners}""");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sporeSac = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA569),
			(e1, s) -> {
				// Spore Sac casts from the actual spores
				s.waitEvents(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56A));
				// Boss casts Sinister Seeds A56D
				// Then there are some map effects:
				/*
					800375C6:20001:5 through 8
					Also 5 through 8 of 200010 but that's too late, might as well just look at pollen cast
				 */
				var pollens = s.waitEventsQuickSuccession(12, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA56B));
				s.waitThenRefreshCombatants(100);
				var outerSafeCorners = EnumSet.copyOf(ArenaSector.quadrants);
				pollens.stream().map(cast -> state.getLatestCombatantData(cast.getSource()))
						.map(pollenPos::forCombatant)
						.forEach(outerSafeCorners::remove);
				if (outerSafeCorners.size() != 2) {
					log.error("bad outerSafeCorners! {}", outerSafeCorners);
				}
				else {
					s.setParam("safeCorners", outerSafeCorners);
					s.setParam("safeInner", outerSafeCorners.stream().map(c -> c.plusQuads(1)).toList());
				}
				s.updateCall(pollenSafe, pollens.get(0));

				/*
					After this:
					Get to safe spot
					Rotate around to not get hit by panto puddles (4 puddles)
					Dodge 8-ways
					Adds spawn
					THey start casting stuff
					You can interrupt winding
					Hurricane force is adds enrage

				 */
			});

}
