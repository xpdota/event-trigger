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
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private final ModifiableCallout<AbilityCastStart> lightOfLife = ModifiableCallout.durationBasedCall("Light of Life", "Big Raidwide");
//	private final ModifiableCallout<AbilityCastStart> hemitheosAeroIV = ModifiableCallout.durationBasedCall("Hemitheos's Aero IV", "Knockback");

	/*
		3308 is short spread
		3397 is the long-timer spread
		3309 is short stack
		3398 is the long stack

	 */
	private final ModifiableCallout<BuffApplied> firstSet_stackSpread = ModifiableCallout.<BuffApplied>durationBasedCall("First Debuff Set: Stack then Spread", "Stack then Spread").statusIcon(3309);
	private final ModifiableCallout<BuffApplied> firstSet_spreadStack = ModifiableCallout.<BuffApplied>durationBasedCall("First Debuff Set: Spread then Stack", "Spread then Stack").statusIcon(3308);
	private final ModifiableCallout<BuffApplied> firstSet_spread = ModifiableCallout.<BuffApplied>durationBasedCall("First Debuff Set: Spread (after stack)", "Spread in Safe Spot").statusIcon(3397);
	private final ModifiableCallout<BuffApplied> firstSet_stack = ModifiableCallout.<BuffApplied>durationBasedCall("First Debuff Set: Stack (after spread)", "Stack in Safe Spot").statusIcon(3398);
	private final ModifiableCallout<BuffApplied> secondAero = new ModifiableCallout<BuffApplied>("Second Debuff Set: Initial Callout", "{nextMechanic}", "{nextMechanic} ({event.estimatedRemainingDuration}) {remainingMechanics}", ModifiableCallout.expiresIn(55)).autoIcon();


	private final ArenaPos arenaPos = new ArenaPos(100, 100, 5, 5);

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
	private final SequentialTrigger<BaseEvent> lightOfLifeSq = SqtTemplates.callWhenDurationIs(
			AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x78E2),
			lightOfLife,
			Duration.ofSeconds(5)
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> firstAeroSet = SqtTemplates.sq(25_000,
			BuffApplied.class,
			e1 -> e1.buffIdMatches(3308, 3397) && e1.getTarget().isThePlayer(),
			(initialBuff, s) -> {
				log.info("First Aero: Start");
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
				BuffApplied followUp = findAnyBuffWithId(stackFirst ? 3397 : 3398);
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

	private BuffApplied findAnyBuffWithId(long id) {
		return buffs.getBuffs().stream()
				.filter(ba -> ba.buffIdMatches(id))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Could not find an existing buff with ID " + id));
	}

	private BuffApplied findAnyBuffWithId(long id, BuffApplied dflt) {
		return buffs.getBuffs().stream()
				.filter(ba -> ba.buffIdMatches(id))
				.findAny()
				.orElse(dflt);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> secondAeroSet = SqtTemplates.sq(70_000,
			BuffApplied.class, e1 -> e1.buffIdMatches(3310),
			(startBuff, s) -> {
				log.info("Second Aero: Start");
				// probably don't need this because we wait for stack 4 which is the last buff
				s.waitMs(200);
				List<BuffApplied> buffs = getBuffs().statusesOnTarget(getState().getPlayer());
				long[] spreadIds = {3310, 3391, 3392, 3393};
				long[] stackIds = {3311, 3394, 3395, 3396};
				boolean[] isSpread = new boolean[4];
				for (BuffApplied buff : buffs) {
					for (int i = 0; i < spreadIds.length; i++) {
						if (buff.getBuff().getId() == spreadIds[i]) {
							isSpread[i] = true;
						}
					}
				}
				List<String> remainingMechanics = new ArrayList<>(4);
				for (boolean spread : isSpread) {
					if (spread) {
						remainingMechanics.add("Spread");
					}
					else {
						remainingMechanics.add("Stack");
					}
				}
				for (int i = 0; i < 4; i++) {
					// TODO: throw the raidwide call in here so that it calls it before the next stack/spread
					boolean nextIsSpread = isSpread[i];
					BuffApplied calloutBuff = findAnyBuffWithId(nextIsSpread ? spreadIds[i] : stackIds[i], startBuff);
					s.updateCall(secondAero.getModified(calloutBuff, Map.of("remainingMechanics", remainingMechanics.subList(i + 1, 4), "nextMechanic", nextIsSpread ? "Spread" : "Stack")));
					long spreadId = spreadIds[i];
					s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(spreadId));
				}
			});

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

		3311 is stack 1
		3394 is stack 2
		3395 is stack 3
		3396 is stack 4
	 */
}
