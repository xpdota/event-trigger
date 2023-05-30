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
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@CalloutRepo(name = "P9S", duty = KnownDuty.P9S)
public class P9S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P9S.class);

	private final XivState state;
	private final StatusEffectRepository buffs;

	public P9S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P9S);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	//	@NpcCastCallout(0x8118)
//	private final ModifiableCallout<AbilityCastStart> ravening = new ModifiableCallout<>("Ravening", "Raidwide");
	@NpcCastCallout(0x814C)
	private final ModifiableCallout<AbilityCastStart> gluttonysAugur = ModifiableCallout.durationBasedCallWithOffset("Gluttnoy's Augur", "Raidwide", Duration.ofMillis(600));

	@NpcCastCallout(0x8151)
	private final ModifiableCallout<AbilityCastStart> duality = new ModifiableCallout<>("Duality of Death", "Tankbuster");

	private final ModifiableCallout<?> dualSpellEarly = new ModifiableCallout<>("Dualspell (Early)", "{buddies ? \"Buddies\" : \"Proteans\"}");
	private final ModifiableCallout<?> dualSpellFull = new ModifiableCallout<>("Dualspell (Full)", "{buddies ? \"Buddies\" : \"Proteans\"} and {out ? \"Out\" : \"In\"}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> dualSpell = SqtTemplates.sq(10_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8154, 0x8155),
			(e1, s) -> {
				log.info("Dualspell: Initial {}", String.format("%X", e1.getAbility().getId()));
				boolean buddies = e1.abilityIdMatches(0x8154);
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
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33127 = new ModifiableCallout<>("Archaic Rockbreaker: Rear+Out then In", "Rear, Out, then In");
	@NpcCastCallout(33128)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33128 = new ModifiableCallout<>("Archaic Rockbreaker: Rear+In then Out", "Rear, In, then Out");
	@NpcCastCallout(33129)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33129 = new ModifiableCallout<>("Archaic Rockbreaker: Front+Out then In", "Front, Out, then In");
	@NpcCastCallout(33130)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker33130 = new ModifiableCallout<>("Archaic Rockbreaker: Front+Out then In", "Front, In, then Out");

	@PlayerHeadmarker(value = -388, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> two = new ModifiableCallout<>("Limit Cut #2", "Two");
	@PlayerHeadmarker(value = -386, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> four = new ModifiableCallout<>("Limit Cut #4", "Four");
	@PlayerHeadmarker(value = -384, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> six = new ModifiableCallout<>("Limit Cut #6", "Six");
	@PlayerHeadmarker(value = -382, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> eight = new ModifiableCallout<>("Limit Cut #8", "Eight");
	@PlayerHeadmarker(value = -138, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> blue = new ModifiableCallout<>("Limit Cut Blue", "Blue");


}
