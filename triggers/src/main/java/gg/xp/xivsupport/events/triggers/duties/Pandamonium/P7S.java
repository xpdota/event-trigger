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
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@CalloutRepo(name = "P7S", duty = KnownDuty.P7S)
public class P7S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P7S.class);
	private final ModifiableCallout<AbilityCastStart> boughOfAttisClose = ModifiableCallout.durationBasedCall("Bough of Attis Attack Close", "Go Far");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisFar = ModifiableCallout.durationBasedCall("Bough of Attis Attack Far", "Get Close");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisLeft = ModifiableCallout.durationBasedCall("Bough of Attis Attack Left", "Go Right");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisRight = ModifiableCallout.durationBasedCall("Bough of Attis Attack Right", "Go Left");
	private final ModifiableCallout<AbilityCastStart> dispersedAeroII = ModifiableCallout.durationBasedCall("Dispersed Aero II", "Tank Spread");
	private final ModifiableCallout<AbilityCastStart> condensedAeroII = ModifiableCallout.durationBasedCall("Condensed Aero II", "Tank Stack");

//	private final ModifiableCallout<AbilityCastStart> hemitheosHoly = ModifiableCallout.durationBasedCall("Hemitheos's Holy", "Spread");
//	private final ModifiableCallout<AbilityCastStart> hemitheosGlareIII = ModifiableCallout.durationBasedCall("Hemitheos's Glare III", "Center");
//	private final ModifiableCallout<AbilityCastStart> immortalsObol = ModifiableCallout.durationBasedCall("Immortal's Obol", "Edge, in Circles");
//	private final ModifiableCallout<AbilityCastStart> hemitheosAeroII = ModifiableCallout.durationBasedCall("Hemitheos's Aero II", "Tankbuster");
	private final ModifiableCallout<AbilityCastStart> sparkOfLife = ModifiableCallout.durationBasedCall("Spark of Life", "Raidwide with Bleed"); //bleed
//	private final ModifiableCallout<AbilityCastStart> staticMoon = ModifiableCallout.durationBasedCall("Static Moon", "Out");
//	private final ModifiableCallout<AbilityCastStart> stymphalianStrike = ModifiableCallout.durationBasedCall("Stymphalian Strike", "Dive");
	private final ModifiableCallout<AbilityCastStart> bladesOfAttis = ModifiableCallout.durationBasedCall("Blades of Attis", "Exaflare");
//	private final ModifiableCallout<AbilityCastStart> hemitheosAeroIV = ModifiableCallout.durationBasedCall("Hemitheos's Aero IV", "Knockback");

	private final ModifiableCallout<BuffApplied> firstSet_stackSpread = ModifiableCallout.durationBasedCall("First Debuff Set: Stack then Spread", "Stack then Spread");
	private final ModifiableCallout<BuffApplied> firstSet_spreadStack = ModifiableCallout.durationBasedCall("First Debuff Set: Spread then Stack", "Spread then Stack");
	private final ModifiableCallout<BuffApplied> firstSet_spread = ModifiableCallout.durationBasedCall("First Debuff Set: Spread (after stack)", "Spread in Safe Spot");
	private final ModifiableCallout<BuffApplied> firstSet_stack = ModifiableCallout.durationBasedCall("First Debuff Set: Stack (after spread)", "Stack in Safe Spot");


	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P7S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	private final StatusEffectRepository buffs;

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P7S);
	}

	private final RepeatSuppressor manyActorsSupp = new RepeatSuppressor(Duration.ofMillis(100));

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x7826 -> call = boughOfAttisFar;
			case 0x7821 -> call = boughOfAttisClose;
			case 0x7824 -> call = boughOfAttisRight;
			case 0x7823 -> call = boughOfAttisLeft;
			case 0x7835 -> call = dispersedAeroII;
			case 0x7836 -> call = condensedAeroII;
			case 0x7839 -> call = sparkOfLife;
			case 0x782E -> call = bladesOfAttis;
			default -> {
				return;
			}
		}
		// 782F is the exaflare fake actor cast, can be used to see pattern
//		if (id == 0x0)
//			call = boughOfAttisClose;
//		else if (id == 0x0) //????+1 fake
//			call = boughOfAttisFar;
//		else if (id == 0x0 && event.getSource().getPos().x() < 100) //????-1 boss
//			call = boughOfAttisLeft;
//		else if (id == 0x0 && event.getSource().getPos().x() > 100) //????-1 boss
//			call = boughOfAttisRight;
//		else if (id == 0x70) //fake x 8 = ????+1 ~1.3 sec after ???? finishes
//			call = hemitheosHoly;
//		else if (id == 0x0) //fake ????+1 ~0.7 after ???? finishes
//			call = hemitheosGlareIII;
//		else if (id == 0x0) //???+1 fake, longer cast. deals damage
//			call = immortalsObol;
//		else if (id == 0x0) //????+1 fake cast x 2 (1 each target)
//			call = hemitheosAeroII;
//		else if (id == 0x0)
//			call = sparkOfLife;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //io out, ????-2 and ????-1 casted to summon eggs
//			call = staticMoon;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //stymphalide dive, ????-2 and ????-1 casted to summon eggs
//			call = stymphalianStrike;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //????-1 real, but instant
//			call = bladesOfAttis;
//		else if (id == 0x0) //????+1 fake, has location
//			call = hemitheosAeroIV;
//		else
//			return;
//
		context.accept(call.getModified(event));
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> firstAeroSet = new SequentialTrigger<>(25_000, BaseEvent.class,
			e1 -> e1 instanceof BuffApplied ba && ba.buffIdMatches(3308, 3397) && ba.getTarget().isThePlayer(),
			(e1, s) -> {
				log.info("First Aero: Start");
				BuffApplied initialBuff = (BuffApplied) e1;
				boolean stackFirst = initialBuff.getBuff().getId() == 3397;
				if (stackFirst) {
					s.updateCall(firstSet_stackSpread.getModified(initialBuff));
				}
				else {
					s.updateCall(firstSet_spreadStack.getModified(initialBuff));
				}
				log.info("First Aero: Waiting");
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(3309));
				// Find the long stack and use that as timing basis
				BuffApplied followUp = getBuffs().getBuffs()
						.stream()
						.filter(ba -> ba.buffIdMatches(3398))
						.findFirst()
						.orElseThrow(() -> new RuntimeException("Couldn't find follow-up buff!"));
				if (stackFirst) {
					// Spread
					s.updateCall(firstSet_spread.getModified(followUp));
				}
				else {
					// Stack
					s.updateCall(firstSet_stack.getModified(followUp));
				}
				log.info("First Aero: Done");
			});

//	@AutoFeed
//	private final SequentialTrigger<BaseEvent> secondAeroSet = new SequentialTrigger<>(25_000, BaseEvent.class,
//			e1 -> e1 instanceof BuffApplied ba && ba.buffIdMatches(3308, 3397) && ba.getTarget().isThePlayer(),
//			(e1, s) -> {
//				log.info("First Aero: Start");
//				BuffApplied initialBuff = (BuffApplied) e1;
//				boolean stackFirst = initialBuff.getBuff().getId() == 3397;
//				if (stackFirst) {
//					s.updateCall(firstSet_stackSpread.getModified(initialBuff));
//				}
//				else {
//					s.updateCall(firstSet_spreadStack.getModified(initialBuff));
//				}
//				log.info("First Aero: Waiting");
//				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(3309));
//				// Find the long stack and use that as timing basis
//				BuffApplied followUp = getBuffs().getBuffs()
//						.stream()
//						.filter(ba -> ba.buffIdMatches(3398))
//						.findFirst()
//						.orElseThrow(() -> new RuntimeException("Couldn't find follow-up buff!"));
//				if (stackFirst) {
//					// Spread
//					s.updateCall(firstSet_spread.getModified(followUp));
//				}
//				else {
//					// Stack
//					s.updateCall(firstSet_stack.getModified(followUp));
//				}
//				log.info("First Aero: Done");
//			});

	/*
		BUFFS
		3308 is short spread
		3397 is the long-timer spread
		3309 is short stack
		3398 is the long stack

		// 1 = 10s, 2 = 25s, 3 = 40s, 4 = 55s
		3310 is aero 1
		3391 is aero 2
		3392 is aero 3
		3393 is aero 4

		3311 is ? 1
		3394 is ? 2
		3395 is ? 3
		3396 is ? 4
	 */
}
