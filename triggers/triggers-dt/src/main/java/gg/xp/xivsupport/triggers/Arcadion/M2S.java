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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "M2S", duty = KnownDuty.M2S)
public class M2S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M2S.class);

	public M2S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private XivState state;
	private StatusEffectRepository buffs;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M2S);
	}

	//
	@NpcCastCallout(0x9183)
	private final ModifiableCallout<AbilityCastStart> raidwide = ModifiableCallout.durationBasedCall("Call Me Honey", "Raidwide");
	@NpcCastCallout(0x919B)
	private final ModifiableCallout<AbilityCastStart> stingingSlash = ModifiableCallout.durationBasedCall("Stinging Slash", "Tank Cleaves");
	@NpcCastCallout(0x919C)
	private final ModifiableCallout<AbilityCastStart> killerSting = ModifiableCallout.durationBasedCall("Killer Sting", "Tank Stack");
//
//	private final SequentialTrigger<BaseEvent>

	private enum PoisonBuff {
		SPREAD,
		BUDDY
	}

	private @Nullable PoisonBuff getPoisonBuff() {
		// TODO: this should also call out when you get the buff
		XivCombatant boss = state.npcById(16941);
		// TODO: This is bad! Both abilities place the same debuff ID, just capture the ability.
		BuffApplied status = buffs.findStatusOnTarget(boss, ba -> ba.buffIdMatches(0xF4B, 0xF61));
		if (status == null) {
			log.warn("No debuff");
			return null;
		}
		AbilityUsedEvent preapp = status.getPreAppAbility();
		if (preapp == null) {
			log.warn("Preapp null");
			return null;
		}
		if (preapp.abilityIdMatches(0x9184, 0x9B0F)) {
			return PoisonBuff.SPREAD;
		}
		else if (preapp.abilityIdMatches(0x9185, 0x9B09)) {
			return PoisonBuff.BUDDY;
		}
		else {
			log.warn("Preapp did not match");
			return null;
		}
	}

	private int getPlayerHeartStacks() {
		// F52 is the 0 stack hidden debuff
		var buff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0xF52, 0xF53, 0xF54, 0xF55, 0xF56));
		if (buff == null) {
			return 0;
		}
		else {
			return (int) (buff.getBuff().getId() - 0xF52);
		}

	}

	private final ModifiableCallout<AbilityCastStart> temptingTwistInitial = ModifiableCallout.durationBasedCall("Tempting Twist: Initial", "In");
//	private final ModifiableCallout<?> temptingTwistAvoidBlobs = new ModifiableCallout<>("Tempting Twist: Initial", "Avoid Blobs");
	private final ModifiableCallout<AbilityCastStart> beelineInitial = ModifiableCallout.durationBasedCall("Beeline: Initial", "Out of Middle");
//	private final ModifiableCallout<?> beelineAvoidBlobs = new ModifiableCallout<>("Tempting Twist: Initial", "In - Avoid Blobs");
	private final ModifiableCallout<?> spread = new ModifiableCallout<>("Poison: Spread", "Spread");
	private final ModifiableCallout<?> buddies = new ModifiableCallout<>("Poison: Stack", "Buddy");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> temptingTwist = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9187, 0x9B11),
			(e1, s) -> {
				s.updateCall(temptingTwistInitial, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				// Wait for it to go off
//				s.updateCall(temptingTwistAvoidBlobs);
				// Wait
				PoisonBuff pb = getPoisonBuff();
//				s.waitMs(1000);
				if (pb == PoisonBuff.SPREAD) {
					s.updateCall(spread);
				}
				else {
					s.updateCall(buddies);
				}

			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> beeline = SqtTemplates.sq(30_000,
			// TODO: 9B0C is unconfirmed
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9186, 0x9B0C),
			(e1, s) -> {
				s.updateCall(beelineInitial, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				// Wait for it to go off
//				s.updateCall(beelineAvoidBlobs);
				// Wait
				PoisonBuff pb = getPoisonBuff();
				if (pb == PoisonBuff.SPREAD) {
					s.updateCall(spread);
				}
				else {
					s.updateCall(buddies);
				}

			});

	@NpcCastCallout(value = 0x91A9, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> beeSting = ModifiableCallout.durationBasedCall("Bee Sting", "Light Parties");

	private final ModifiableCallout<?> outCards = new ModifiableCallout<>("Center/Outer Stage: Out+Cardinals", "Out+Cardinals");
//	private final ModifiableCallout<?> outInter = new ModifiableCallout<>("Center/Outer Stage: Out+Intercards", "Out+Intercards");
//	private final ModifiableCallout<?> inCards = new ModifiableCallout<>("Center/Outer Stage: In+Cardinals", "In+Cardinals");
	private final ModifiableCallout<?> inInter = new ModifiableCallout<>("Center/Outer Stage: In+Intercards", "In+Intercards");
	private final ModifiableCallout<?> cross = new ModifiableCallout<>("Center/Outer Stage: Cross", "Out+Intercards");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> centerStage = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91AC),
			(e1, s) -> {
				s.updateCall(inInter);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(cross);
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(outCards);
				// TODO: are these always cards/inter?
				// in + intercards
				// out + intercards
				// out + cards
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> outerStage = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91AD),
			(e1, s) -> {
				s.updateCall(outCards);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(cross);
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(inInter);
				// TODO: are these always cards/inter?
				// out + cards
				// out + intercards
				// in + intercards
			});

	private final ModifiableCallout<AbilityCastStart> hbl1Initial = ModifiableCallout.durationBasedCall("Honey B. Live: 1st Beat", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> hbl2Initial = ModifiableCallout.durationBasedCall("Honey B. Live: 2nd Beat", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> hbl3Initial = ModifiableCallout.durationBasedCall("Honey B. Live: 3rd Beat", "Raidwide");
	@NpcCastCallout(0x918F)
	private final ModifiableCallout<AbilityCastStart> hbFinale = ModifiableCallout.durationBasedCall("Honey B. Finale", "Raidwide");

	@NpcCastCallout(0x9B7E)
	private final ModifiableCallout<AbilityCastStart> loveseeker = ModifiableCallout.durationBasedCall("Loveseeker", "Out");

	// TODO: red color for hard enrages?
	@NpcCastCallout(0x91B7)
	private final ModifiableCallout<AbilityCastStart> enrage = ModifiableCallout.durationBasedCall("Sheer Heart Attack", "Enrage");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> hbl1 = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C24),
			(e1, s) -> {
				s.updateCall(hbl1Initial, e1);
				// Starts with center/outer
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x919F));
				// Call out to take towers if stack count remains below 2
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x919F));
				// Call out to stack if stacks < 3
			});
	@AutoFeed
	private final SequentialTrigger<BaseEvent> hbl2 = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C25),
			(e1, s) -> {
				s.updateCall(hbl2Initial, e1);
			});
	@AutoFeed
	private final SequentialTrigger<BaseEvent> hbl3 = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C26),
			(e1, s) -> {
				s.updateCall(hbl3Initial, e1);
			});
	@AutoFeed
	private final SequentialTrigger<BaseEvent> rotten = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C26),
			(e1, s) -> {
//				s.updateCall(hbl3Initial, e1);
			});

	private final ModifiableCallout<AbilityCastStart> alarm1initial = ModifiableCallout.durationBasedCall("Alarm Pheremones 1: Initial", "Bait Lines");
	private final ModifiableCallout<AbilityCastStart> alarm2initial = ModifiableCallout.durationBasedCall("Alarm Pheremones 2: Initial", "Bait Lines and Puddles");
	private final ModifiableCallout<HeadMarkerEvent> alarm2puddle = new ModifiableCallout<>("Alarm Pheremones 2: Puddle on you");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> alarmPheremones = SqtTemplates.multiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x917D),
			(e1, s) -> {
				s.updateCall(alarm1initial);
				// First set seems yolo?
			}, (e1, s) -> {
				s.updateCall(alarm2initial);
				// Second set seems to be the one with coordinated stuff
				var hm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().isThePlayer());
				s.updateCall(alarm2puddle);
			});
	// all 3 lives have initial raidwides
	// they also end with a finale which is another raidwide
	/*
	Alarm pheremones
	#1 is 917D
	#2 is 917D
	#1 is yolo
	#2 is puddles plus healer stacks
	 */
	/*
	Live 1 0x9C24
	Debuff F53 and up
	Cycle
	Take towers if you can
	Dodge hearts
	Players with <3 take stack
	 */
	/*
	Live 2
	Party stacks with aoes
	Spreads with towers
	Beeline/in
	TODO: are these always spread
	 */
	/*
	Live 3
		Debuffs go out
		Cycle
		Everyone else middle
		Defamations THEN tower
		Cycle
		Defamations THEN tower
		Beeline or in
	 */

	/*
	Rotten:
	Do not be stacked at any point
	It has nisis
	Join alpha to beta based on debuff timers
	There is a magic vuln, so wait for next one to expire
	Call me honey casts during it (raidwides)
	 */
}
