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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcAbilityUsedCallout;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@CalloutRepo(name = "P9S", duty = KnownDuty.P9S)
public class P9S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P9S.class);

	private XivState state;
	private final StatusEffectRepository buffs;
	private final ArenaPos ap = new ArenaPos(100.0, 100.0, 3, 3);

	public P9S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P9S);
	}

	@NpcAbilityUsedCallout({0x8118, 0x8119, 0x811a, 0x817b})
	private final ModifiableCallout<AbilityCastStart> ravening = new ModifiableCallout<>("Ravening", "Raidwide");
	@NpcCastCallout(0x814C)
	private final ModifiableCallout<AbilityCastStart> gluttonysAugur = ModifiableCallout.durationBasedCallWithOffset("Gluttnoy's Augur", "Raidwide", Duration.ofMillis(600));

	@NpcCastCallout(0x8151)
	private final ModifiableCallout<AbilityCastStart> duality = new ModifiableCallout<>("Duality of Death", "Tankbuster");

	@NpcCastCallout(0x8186)
	private final ModifiableCallout<AbilityCastStart> beastlyFury = ModifiableCallout.durationBasedCall("Beatly Fury", "Raidwide");

	private final ModifiableCallout<?> dualSpellEarly = new ModifiableCallout<>("Dualspell/Two Minds (Early)", "{buddies ? \"Buddies\" : \"Proteans\"}");
	private final ModifiableCallout<?> dualSpellFull = new ModifiableCallout<>("Dualspell/Two Minds (Full)", "{buddies ? \"Buddies\" : \"Proteans\"} and {out ? \"Out\" : \"In\"}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> dualSpell = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8154, 0x8155, 0x8184, 0x8185),
			(e1, s) -> {
				// Two minds:
				// in buddies: 8144 8184 (cast) 8123
				// out buddies: 8144 8184 (cast) 8122
				// out prot: 8144 8185 (cast) 8123
				log.info("Dualspell: Initial {}", String.format("%X", e1.getAbility().getId()));
				boolean buddies = e1.abilityIdMatches(0x8154, 0x8184);
				s.setParam("buddies", buddies);
				s.updateCall(dualSpellEarly);
				AbilityUsedEvent inOut = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8122, 0x8123, 0x815C));
				log.info("Dualspell: Followup {}", String.format("%X", inOut.getAbility().getId()));
				boolean out = inOut.abilityIdMatches(0x8122, 0x815C);
				s.setParam("out", out);
				s.updateCall(dualSpellFull);
			});

	@NpcCastCallout(0x815F)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker = new ModifiableCallout<>("Archaic Rockbreaker", "Knockback");

	@NpcCastCallout(33127)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33127 = new ModifiableCallout<>("Archaic Rockbreaker: Rear+Out then In", "Out, Rear, then In");
	@NpcCastCallout(33128)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33128 = new ModifiableCallout<>("Archaic Rockbreaker: Rear+In then Out", "In, Rear, then Out");
	@NpcCastCallout(33129)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33129 = new ModifiableCallout<>("Archaic Rockbreaker: Front+Out then In", "Out, Front, then In");
	@NpcCastCallout(33130)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33130 = new ModifiableCallout<>("Archaic Rockbreaker: Front+Out then In", "In, Front, then Out");
	@NpcCastCallout(0x816D)
	private final ModifiableCallout<AbilityCastStart> archaicDemolish = ModifiableCallout.durationBasedCallWithOffset("Archaic Demolish", "Light Parties", Duration.ofMillis(1100));
	@NpcCastCallout(0x816F)
	private final ModifiableCallout<AbilityCastStart> ascendantFist = ModifiableCallout.durationBasedCall("Ascendant Fist", "Tankbuster");

	private final ModifiableCallout<HeadMarkerEvent> two = new ModifiableCallout<>("Limit Cut: #2", "Two");
	private final ModifiableCallout<HeadMarkerEvent> four = new ModifiableCallout<>("Limit Cut: #4", "Four");
	private final ModifiableCallout<HeadMarkerEvent> six = new ModifiableCallout<>("Limit Cut: #6", "Six");
	private final ModifiableCallout<HeadMarkerEvent> eight = new ModifiableCallout<>("Limit Cut: #8", "Eight");
	private final ModifiableCallout<?> clockwise = new ModifiableCallout<>("Limit Cut: Clockwise", "Clockwise, Starting {start}");
	private final ModifiableCallout<?> counterClockwise = new ModifiableCallout<>("Limit Cut: Counter-Clockwise", "Counter-Clockwise, Starting {start}");
	private final ModifiableCallout<?> nothing = new ModifiableCallout<>("Limit Cut: Nothing", "Nothing");
	private final ModifiableCallout<?> genericTwo = new ModifiableCallout<>("Limit Cut: First Round", "Two");
	private final ModifiableCallout<?> genericFour = new ModifiableCallout<>("Limit Cut: Second Round", "Four");
	private final ModifiableCallout<?> genericSix = new ModifiableCallout<>("Limit Cut: Third Round", "Six");
	private final ModifiableCallout<?> genericEight = new ModifiableCallout<>("Limit Cut: Fourth Round", "Eight");
	private final ModifiableCallout<?> baitPunchNow = new ModifiableCallout<>("Limit Cut: Bait Punch Now", "Bait Punch");
	private final ModifiableCallout<?> takeTowerNow = new ModifiableCallout<>("Limit Cut: Take Tower Now", "Take Tower");
	@PlayerHeadmarker(value = -138, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> blue = new ModifiableCallout<>("Limit Cut: Blue Now", "Defamation");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitCut1 = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x817C),
			(e1, s) -> {
				List<HeadMarkerEvent> markers = s.waitEvents(8, HeadMarkerEvent.class, hme -> true);
				// First four are 2/4/6/8 player markers, last four are 1/3/5/7 non-player markers
				var calls = List.of(two, four, six, eight);
				int playerNumber = 0;
				for (int i = 0; i < 4; i++) {
					HeadMarkerEvent marker = markers.get(i);
					if (marker.getTarget().isThePlayer()) {
						s.updateCall(calls.get(i));
						playerNumber = (i + 1) * 2;
					}
				}
				if (playerNumber == 0) {
					s.updateCall(nothing);
				}
				s.waitThenRefreshCombatants(1000);
				XivCombatant oneBall = state.getLatestCombatantData(markers.get(5).getTarget());
				XivCombatant twoBall = state.getLatestCombatantData(markers.get(6).getTarget());
				ArenaSector start = ap.forCombatant(oneBall);
				ArenaSector second = ap.forCombatant(twoBall);
				s.setParam("start", start);
				int eighths = start.eighthsTo(second);
				if (eighths > 0) {
					s.call(clockwise);
				}
				else if (eighths < 0) {
					s.call(counterClockwise);
				}
				else {
					log.error("Could not determine rotation direction: {} -> {}", start, second);
				}
				s.waitMs(2_000);
				var genericCalls = List.of(genericTwo, genericFour, genericSix, genericEight);
				for (int i = 2; i <= 8; i += 2) {
					log.info("Number: {}", i);
					s.setParam("i", i);
					s.updateCall(genericCalls.get((i / 2) - 1));
					s.waitMs(1000);
					if (playerNumber != 0) {
						if (i == playerNumber) {
							s.updateCall(baitPunchNow);
						}
						else if (i % 4 == playerNumber % 4) {
							s.updateCall(takeTowerNow);
						}
					}
					if (i < 8) {
						s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8180));
					}
				}
			}
	);
	private final ModifiableCallout<?> charybdisMiddle = new ModifiableCallout<>("Charybdis: Wait Middle", "Wait Middle");
	private final ModifiableCallout<?> charybdisFirstSet = new ModifiableCallout<>("Charybdis: First Set", "First Set");
	private final ModifiableCallout<?> charybdisSecondSet = new ModifiableCallout<>("Charybdis: Second Set", "Second Set");
	private final ModifiableCallout<?> charybdisHide = new ModifiableCallout<>("Charybdis: Hide Behind Meteor", "Hide Behind Meteor");
	private final ModifiableCallout<AbilityCastStart> charybdisFinalMeteorBurst = ModifiableCallout.durationBasedCall("Charybdis: Burst", "Away from Meteor");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> charybdis = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8170),
			(e1, s) -> {
				s.updateCall(charybdisMiddle);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8173));
				s.updateCall(charybdisFirstSet);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8178));
				s.updateCall(charybdisSecondSet);
				s.waitMs(1_000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8178));
				s.updateCall(charybdisHide);
				// Wait for ecliptic meteor to start casting
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8175));
				// Then call the next Burst cast
				AbilityCastStart burst = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8174));
				s.updateCall(charybdisFinalMeteorBurst, burst);
			});

	private final ModifiableCallout<HeadMarkerEvent> chimericNumber1 = new ModifiableCallout<>("Chimeric Succession: #1", "One");
	private final ModifiableCallout<HeadMarkerEvent> chimericNumber2 = new ModifiableCallout<>("Chimeric Succession: #2", "Two");
	private final ModifiableCallout<HeadMarkerEvent> chimericNumber3 = new ModifiableCallout<>("Chimeric Succession: #3", "Three");
	private final ModifiableCallout<HeadMarkerEvent> chimericNumber4 = new ModifiableCallout<>("Chimeric Succession: #4", "Four");
	private final ModifiableCallout<?> chimericSwap = new ModifiableCallout<>("Chimeric Succession: Swap Now", "Swap");
	private final ModifiableCallout<?> chimericNothing = new ModifiableCallout<>("Chimeric Succession: Nothing", "Nothing");
	private final ModifiableCallout<AbilityCastStart> chimericBehind = ModifiableCallout.durationBasedCall("Chimeric Succession: Behind Boss", "Behind Boss");
	private final ModifiableCallout<AbilityCastStart> chimericInFront = ModifiableCallout.durationBasedCall("Chimeric Succession: In Front of Boss", "Front of Boss");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitCut2 = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x81BB),
			(e1, s) -> {
				List<HeadMarkerEvent> markers = s.waitEvents(4, HeadMarkerEvent.class, hme -> true);
				// First four are 2/4/6/8 player markers, last four are 1/3/5/7 non-player markers
				var calls = List.of(chimericNumber1, chimericNumber2, chimericNumber3, chimericNumber4);
				int playerNumber = 0;
				for (int i = 0; i < 4; i++) {
					HeadMarkerEvent marker = markers.get(i);
					if (marker.getTarget().isThePlayer()) {
						s.updateCall(calls.get(i));
						playerNumber = i + 1;
					}
				}
				if (playerNumber == 0) {
					s.updateCall(chimericNothing);
				}
				s.waitMs(2_000);
				if (playerNumber != 0) {
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8183, 0x8190, 0x8191, 0x8192) && aue.isFirstTarget());
					if (playerNumber == 1 || playerNumber == 3) {
						s.updateCall(chimericSwap);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8183, 0x8190, 0x8191, 0x8192) && aue.isFirstTarget());
					if (playerNumber == 2 || playerNumber == 4) {
						s.updateCall(chimericSwap);
					}
				}
				AbilityCastStart followup = s.waitEvent(AbilityCastStart.class, acs -> acs.getSource().npcIdMatches(16087));
				if (followup.abilityIdMatches(0x8795)) {
					s.updateCall(chimericBehind, followup);
				}
				else {
					s.updateCall(chimericInFront, followup);
				}
			}
	);

}
