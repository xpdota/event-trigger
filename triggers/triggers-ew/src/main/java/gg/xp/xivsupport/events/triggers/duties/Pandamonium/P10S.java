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
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "P10S", duty = KnownDuty.P10S)
public class P10S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P10S.class);

	private final XivState state;
	private StatusEffectRepository buffs;
	private final ArenaPos ap = new ArenaPos(100, 100, 6, 6);

	public P10S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P10S);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@NpcCastCallout(0x82A5)
	private final ModifiableCallout<AbilityCastStart> ultima = ModifiableCallout.<AbilityCastStart>durationBasedCall("Ultima", "Raidwide with Bleed").statusIcon(0x828);

	@NpcCastCallout(0x829F)
	private final ModifiableCallout<AbilityCastStart> soulGrasp = ModifiableCallout.durationBasedCall("Soul Grasp", "Tank Stack, Multiple Hits");

	@NpcCastCallout(0x827C)
	private final ModifiableCallout<AbilityCastStart> silkSpit = ModifiableCallout.durationBasedCall("Silkspit", "Spread");

	private final ModifiableCallout<AbilityCastStart> dividingWings = ModifiableCallout.durationBasedCall("Dividing Wings", "Groups and Cleaves");
	private final ModifiableCallout<AbilityCastStart> dividingWings2 = ModifiableCallout.durationBasedCall("Dividing Wings", "Groups and Cleaves on Sides");
	private final ModifiableCallout<AbilityCastStart> dividingWings3 = ModifiableCallout.durationBasedCall("Dividing Wings", "Stack and Build Web");

	private final ModifiableCallout<TetherEvent> dividingWingsTether = new ModifiableCallout<>("Dividing Wings Tether", "Bait Cleave");
	private final ModifiableCallout<?> dividingWingsNoTether = new ModifiableCallout<>("Dividing Wings No Tether", "Stack");
	private final ModifiableCallout<?> dividingWingsBreakChains = new ModifiableCallout<>("Dividing Wings Chain Break", "Break Chains");
	private final ModifiableCallout<?> dividingWingsRearWeb = new ModifiableCallout<>("Dividing Wings Make Web", "Make Web, South Edge");

	private final ModifiableCallout<HeadMarkerEvent> meltdownHasMarker = new ModifiableCallout<>("Meltdown: Have Spread Marker", "Spread");
	private final ModifiableCallout<AbilityCastStart> meltdownNoMarker = ModifiableCallout.durationBasedCall("Meltdown: No Spread Marker", "Stack");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> dividingWingsSq = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8297),
			(e1, s) -> {
		log.info("Dividing Wings 1: Start");
				s.updateCall(dividingWings, e1);
				List<TetherEvent> tethers = s.waitEvents(2, TetherEvent.class, te -> te.tetherIdMatches(242));
				// TODO: tether location
				Optional<TetherEvent> myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
				if (myTether.isPresent()) {
					s.updateCall(dividingWingsTether, myTether.get());
				}
				else {
					s.updateCall(dividingWingsNoTether);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x827F));
				if (myTether.isPresent()) {
					s.updateCall(dividingWingsBreakChains);
				}
			}, (e1, s) -> {
				log.info("Dividing Wings 2: Start");
				s.updateCall(dividingWings2, e1);
				List<TetherEvent> tethers = s.waitEvents(2, TetherEvent.class, te -> te.tetherIdMatches(242));
				// TODO: tether location
				Optional<TetherEvent> myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
				if (myTether.isPresent()) {
					s.updateCall(dividingWingsTether, myTether.get());
				}
				else {
					s.updateCall(dividingWingsNoTether);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x827F));
				if (myTether.isPresent()) {
					s.updateCall(dividingWingsBreakChains);
				}
				log.info("Meltdown: Start");
				List<HeadMarkerEvent> headMarks = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -444, Duration.ofMillis(100));
				log.info("Meltdown: Got HMs");
				AbilityCastStart linestack = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x829D));
				log.info("Meltdown: Got Line Stack");
				Optional<HeadMarkerEvent> myMarker = headMarks.stream().filter(hme -> hme.getTarget().isThePlayer()).findAny();
				if (myMarker.isPresent()) {
					s.updateCall(meltdownHasMarker, myMarker.get());
				}
				else {
					s.updateCall(meltdownNoMarker, linestack);
				}
				// Bonds are handled elsewhere
			}, (e1, s) -> {
				log.info("Dividing Wings 3: Start");
				s.updateCall(dividingWings3, e1);
				{
					List<TetherEvent> tethers = s.waitEvents(2, TetherEvent.class, te -> te.tetherIdMatches(242));
					List<HeadMarkerEvent> hms = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hm -> true);
					// TODO: tether location
					Optional<TetherEvent> myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
					Optional<HeadMarkerEvent> myHm = hms.stream().filter(hm -> hm.getTarget().isThePlayer()).findFirst();
					boolean callBreak = false;
					if (myTether.isPresent()) {
						s.updateCall(dividingWingsTether, myTether.get());
					}
					else if (myHm.isPresent() && myHm.get().getMarkerOffset() == -37) {
						s.updateCall(dividingWingsRearWeb);
						callBreak = true;
					}
					else {
						s.updateCall(dividingWingsNoTether);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8298));
					if (callBreak) {
						s.updateCall(dividingWingsBreakChains);
					}
				}
				{
					List<HeadMarkerEvent> hms = s.waitEventsQuickSuccession(3, HeadMarkerEvent.class, hm -> hm.getMarkerOffset() == -37);
					Optional<HeadMarkerEvent> myHm = hms.stream().filter(hm -> hm.getTarget().isThePlayer()).findFirst();
					if (myHm.isPresent()) {
						s.updateCall(dividingWingsRearWeb);
					}
				}


			});

	@NpcCastCallout(0x82A6)
	private final ModifiableCallout<AbilityCastStart> holy = ModifiableCallout.durationBasedCall("Panda Holy", "Out");
	@NpcCastCallout(0x82A7)
	private final ModifiableCallout<AbilityCastStart> circles = ModifiableCallout.durationBasedCall("Circles of Panda", "In");
	@NpcCastCallout(0x829A)
	private final ModifiableCallout<AbilityCastStart> wickedStep = ModifiableCallout.durationBasedCall("Wicked Step", "Tank Towers with Knockback");

	//	@HandleEvents
//	public void tethers(EventContext context, TetherEvent tether) {
//		if (!tether.eitherTargetMatches(XivCombatant::isThePlayer)) {
//			return;
//		}
//		switch ((int) tether.getId()) {
//			case 0xF2 -> context.accept(dividingWingsTether.getModified(tether));
//		}
//	}
	private final ModifiableCallout<HeadMarkerEvent> web1webOnMe = new ModifiableCallout<>("Entangling Web 1: Web on You", "Make Bridge");
	private final ModifiableCallout<HeadMarkerEvent> web1noWebOnMe = new ModifiableCallout<>("Entangling Web 1: No web on you", "No Web");
	private final ModifiableCallout<AbilityCastStart> web1bury = ModifiableCallout.durationBasedCall("Entangling Web 1: Bury", "Soak Towers");

	private final ModifiableCallout<AbilityCastStart> web2bury = ModifiableCallout.durationBasedCall("Entangling Web 2: Bury", "Soak Towers");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> entanglingWebSq = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8722),
			(e1, s) -> {
				log.info("Entangling Web 1: Start");
				List<HeadMarkerEvent> webs = s.waitEventsQuickSuccession(4, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -37, Duration.ofMillis(100));
				Optional<HeadMarkerEvent> webOnMe = webs.stream().filter(hme -> hme.getTarget().isThePlayer()).findFirst();
				if (webOnMe.isPresent()) {
					s.updateCall(web1webOnMe, webOnMe.get());
				}
				else {
					s.updateCall(web1noWebOnMe);
				}
				AbilityCastStart explosion = s.waitEvent(AbilityCastStart.class, event -> event.abilityIdMatches(0x8282));
				s.updateCall(web1bury, explosion);
			}, (e1, s) -> {
				AbilityCastStart explosion = s.waitEvent(AbilityCastStart.class, event -> event.abilityIdMatches(0x8282));
				s.updateCall(web2bury, explosion);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8287));
				daemoniacBondsHelper(s);
			});

	private final ModifiableCallout<BuffApplied> bondsStackThenSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Stack -> Spread", "Stack then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> bondsBuddyThenSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Buddy -> Spread", "Buddy then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> bondsSpreadThenStack = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Spread -> Stack", "Spread then Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> bondsSpreadThenBuddy = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Spread -> Buddy", "Spread then Buddy").autoIcon();
	private final ModifiableCallout<BuffApplied> bondsStack = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Stack", "Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> bondsSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Spread", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> bondsBuddy = ModifiableCallout.<BuffApplied>durationBasedCall("Daemoniac Bonds: Buddy", "Buddy").autoIcon();

	// Call this when buffs are already out
	private void daemoniacBondsHelper(SequentialTriggerController<?> s) {
		BuffApplied spreadBuff = buffs.findBuffById(0xDDE);
		BuffApplied buddyBuff = buffs.findBuffById(0xDDF);
		BuffApplied stackBuff = buffs.findBuffById(0xE70);
		if (spreadBuff == null) {
			log.error("Daemoniac Bonds: Missing Spread Buff!");
			return;
		}
		if (buddyBuff == null && stackBuff == null) {
			log.error("Daemoniac Bonds: Missing stack/buddy buff!");
			return;
		}
		if (buddyBuff != null && stackBuff != null) {
			log.error("Daemoniac Bonds: Got both stack and buddy!");
			return;
		}
		if (stackBuff != null) {
			if (spreadBuff.getInitialDuration().compareTo(stackBuff.getInitialDuration()) > 0) {
				s.updateCall(bondsStackThenSpread, stackBuff);
				s.waitBuffRemoved(buffs, stackBuff);
				s.updateCall(bondsSpread, spreadBuff);
			}
			else {
				s.updateCall(bondsSpreadThenStack, spreadBuff);
				s.waitBuffRemoved(buffs, spreadBuff);
				s.updateCall(bondsStack, stackBuff);
			}
		}
		else {
			if (spreadBuff.getInitialDuration().compareTo(buddyBuff.getInitialDuration()) > 0) {
				s.updateCall(bondsBuddyThenSpread, buddyBuff);
				s.waitBuffRemoved(buffs, buddyBuff);
				s.updateCall(bondsSpread, spreadBuff);
			}
			else {
				s.updateCall(bondsSpreadThenBuddy, spreadBuff);
				s.waitBuffRemoved(buffs, spreadBuff);
				s.updateCall(bondsBuddy, buddyBuff);
			}
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> meltdown = SqtTemplates.multiInvocation(90_000,
			// This actually starts on Bonds since it needs to capture headmarkers earlier
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82A1),
			(e1, s) -> {
				log.info("Meltdown: Start");
				List<HeadMarkerEvent> headMarks = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -444, Duration.ofMillis(100));
				log.info("Meltdown: Got HMs");
				AbilityCastStart linestack = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x829D));
				log.info("Meltdown: Got Line Stack");
				Optional<HeadMarkerEvent> myMarker = headMarks.stream().filter(hme -> hme.getTarget().isThePlayer()).findAny();
				if (myMarker.isPresent()) {
					s.updateCall(meltdownHasMarker, myMarker.get());
				}
				else {
					s.updateCall(meltdownNoMarker, linestack);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(linestack.getAbility().getId()));
				daemoniacBondsHelper(s);
			},
			(e1, s) -> {
			}
	);

	private final ModifiableCallout<AbilityCastStart> turretsSoak = ModifiableCallout.durationBasedCall("Turrets: Soak Towers", "Soak Towers");
	private final ModifiableCallout<?> turretsBait1 = new ModifiableCallout<>("Turrets: Round 1", "First");
	private final ModifiableCallout<?> turretsBait2 = new ModifiableCallout<>("Turrets: Round 2", "Second");
	private final ModifiableCallout<?> turretsBait3 = new ModifiableCallout<>("Turrets: Round 3", "Third");
	private final ModifiableCallout<?> turretsBait4 = new ModifiableCallout<>("Turrets: Round 4", "Fourth");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> turrets = SqtTemplates.sq(90_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x87AF),
			(e1, s) -> {
				log.info("Turrets Start");
				AbilityCastStart soakCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8282));
				s.updateCall(turretsSoak, soakCast);
				s.waitMs(8_000);
				List<ModifiableCallout<?>> baitCalls = List.of(turretsBait1, turretsBait2, turretsBait3, turretsBait4);
				for (int i = 0; i < 4; i++) {
					s.updateCall(baitCalls.get(i));
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8288));
					s.waitMs(200);
				}
				daemoniacBondsHelper(s);
			}
	);

	private final ModifiableCallout<AbilityCastStart> pandaRay = ModifiableCallout.durationBasedCall("Panda Ray", "{safe} Safe");
	private final ModifiableCallout<?> avoidLines = new ModifiableCallout<>("Avoid Lines");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cleaveAndLasers = SqtTemplates.sq(15_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8289, 0x828B),
			(e1, s) -> {
				// Panda ray - 828B + 828A is west safe, ? is east safe
				if (e1.abilityIdMatches(0x828B)) {
					s.setParam("safe", ArenaSector.WEST);
				}
				else {
					s.setParam("safe", ArenaSector.EAST);
				}
				s.updateCall(pandaRay, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(e1.getAbility().getId()));
				s.updateCall(avoidLines);
			});

	private final ModifiableCallout<AbilityCastStart> harrowingHell = ModifiableCallout.durationBasedCall("Harrowing Hell", "Heavy Raidwides, Tanks in Front");
	private final ModifiableCallout<AbilityCastStart> harrowingHellKb = ModifiableCallout.durationBasedCall("Harrowing Hell KB", "Knockback");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> harrowingHellSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x828F),
			(e1, s) -> {
				s.updateCall(harrowingHell, e1);
				AbilityCastStart kb = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8294));
				s.updateCall(harrowingHellKb, kb);
				s.waitMs(2_000);
				daemoniacBondsHelper(s);
			});

	@NpcCastCallout(0x8295)
	private final ModifiableCallout<AbilityCastStart> partedPlumes = ModifiableCallout.durationBasedCall("Parted Plumes", "Spin");

}
