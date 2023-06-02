package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.HeadmarkerOffsetTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.MonitorInfo;
import java.util.ArrayList;
import java.util.List;

@CalloutRepo(name = "P12S Doorboss", duty = KnownDuty.P12S)
public class P12SDoorBoss extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P12SDoorBoss.class);

	private final XivState state;
	private final StatusEffectRepository buffs;
	private HeadmarkerOffsetTracker hmot;

	public P12SDoorBoss(XivState state, StatusEffectRepository buffs, HeadmarkerOffsetTracker hmot) {
		this.state = state;
		this.buffs = buffs;
		this.hmot = hmot;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P12S);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@NpcCastCallout(0x8304)
	private final ModifiableCallout<?> onTheSoul = ModifiableCallout.durationBasedCall("On the Soul", "Raidwide");

	// Trigger to fix headmarker offsets with some fakery
	@AutoFeed
	private final SequentialTrigger<BaseEvent> hmoffFixer = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82E8),
			(e1, s) -> {
				ArenaSector firstSafe = e1.abilityIdMatches(0x82E8) ? ArenaSector.EAST : ArenaSector.WEST;
				HeadMarkerEvent firstHm = s.waitEvent(HeadMarkerEvent.class, hm -> true);
				if (firstSafe == ArenaSector.WEST && firstHm.getMarkerOffset() == 0) {
					hmot.setFakeFirstId(firstHm.getMarkerId() - 1);
				}
			});

	private final ModifiableCallout<?> trinitySafeSpots = new ModifiableCallout<>("Trinity Safe Spots", "{safespots[0]}, {safespots[1]}, {safespots[2]}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> trinityOfSouls = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82E7, 0x82E8),
			(e1, s) -> {
					/*
						Notes: Seems to be cast -> headmarkers during cast
						Notes indicate SAFE SPOTS
						82E8, 490 488 497 = right left left, flipping (8AB)
						82E8, 490 488 497 = right left left, flipping (8AB)
						82E7, 491 488 497 = left left left, flipping (7AB)
						82E7, 491 488 497 = left left left, flipping (7AB)
						82E7, 491 488 496 = left left right, flipping (7AC)
						82E8, 490 489 496 = right right? right?, flipping (89C)
						82E7, 491 489 496 = left right right, flipping (79C)

						82E7 = left safe first
						82E8 = right safe first
						-1 = left safe (must consider flip)
						-2 = right safe (must consider flip)
						-3 = left safe
						-4 = right safe
					 */
				List<ArenaSector> safeSpots = new ArrayList<>(3);
				safeSpots.add(e1.abilityIdMatches(0x82E8) ? ArenaSector.EAST : ArenaSector.WEST);
				List<HeadMarkerEvent> hms = s.waitEvents(3, HeadMarkerEvent.class, hm -> true);
				boolean flipping = true;
				HeadMarkerEvent secondHm = hms.get(1);
				ArenaSector secondSafe = switch (secondHm.getMarkerOffset()) {
					case -1 -> ArenaSector.WEST;
					case -2 -> ArenaSector.EAST;
					default -> ArenaSector.UNKNOWN;
				};
				if (flipping) {
					secondSafe = secondSafe.opposite();
				}
				safeSpots.add(secondSafe);
				HeadMarkerEvent thirdHm = hms.get(2);
				ArenaSector thirdSafe = switch (thirdHm.getMarkerOffset()) {
					case -3 -> ArenaSector.WEST;
					case -4 -> ArenaSector.EAST;
					default -> ArenaSector.UNKNOWN;
				};
				safeSpots.add(thirdSafe);
				// TODO: test
				s.setParam("safeSpots", safeSpots);
				s.updateCall(trinitySafeSpots);

			});
}
