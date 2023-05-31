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
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
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

	private final ModifiableCallout<AbilityCastStart> dividingWings = ModifiableCallout.durationBasedCall("Dividing Wings", "Groups and Cleaves");

	private final ModifiableCallout<TetherEvent> dividingWingsTether = new ModifiableCallout<>("Dividing Wings Tether", "Bait Cleave");
	private final ModifiableCallout<?> dividingWingsNoTether = new ModifiableCallout<>("Dividing Wings No Tether", "Stack");
	private final ModifiableCallout<?> dividingWingsBreakChains = new ModifiableCallout<>("Dividing Wings Chain Break", "Break Chains");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> dividingWingsSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8297),
			(e1, s) -> {
				s.updateCall(dividingWings, e1);
				List<TetherEvent> tethers = s.waitEvents(2, TetherEvent.class, te -> te.tetherIdMatches(242));
				Optional<TetherEvent> myTether = tethers.stream().filter(te -> te.eitherTargetMatches(XivCombatant::isThePlayer)).findFirst();
				if (myTether.isPresent()) {
					s.updateCall(dividingWingsTether, myTether.get());
				}
				else {
					s.updateCall(dividingWingsNoTether);
				}
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x827F));
				if (myTether.isPresent()) {
					s.updateCall(dividingWingsBreakChains);
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
			});

	private final ModifiableCallout<HeadMarkerEvent> bondsHasMarker = new ModifiableCallout<>("Daemoniac Bonds: Have Head Marker", "Spread");
	private final ModifiableCallout<AbilityCastStart> bondsNoMarker = ModifiableCallout.durationBasedCall("Daemoniac Bonds: Have Head Marker", "Spread");
	private final ModifiableCallout<BuffApplied> bondsStackFirst = ModifiableCallout.durationBasedCall("Daemoniac Bonds: Stack First", "Stack then Spread");
	private final ModifiableCallout<BuffApplied> bondsSpreadFirst = ModifiableCallout.durationBasedCall("Daemoniac Bonds: Spread First", "Spread then Stack");
	private final ModifiableCallout<BuffApplied> bondsStack = ModifiableCallout.durationBasedCall("Daemoniac Bonds: Stack", "Stack");
	private final ModifiableCallout<BuffApplied> bondsSpread = ModifiableCallout.durationBasedCall("Daemoniac Bonds: Spread", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> deamoniacBonds = SqtTemplates.sq(90_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x82A1),
			(e1, s) -> {
				log.info("Daemoniac Bonds: Start");
				List<HeadMarkerEvent> headMarks = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == -444, Duration.ofMillis(100));
				log.info("Daemoniac Bonds: Got HMs");
				AbilityCastStart linestack = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x829D));
				log.info("Daemoniac Bonds: Got Line Stack");
				Optional<HeadMarkerEvent> myMarker = headMarks.stream().filter(hme -> hme.getTarget().isThePlayer()).findAny();
				if (myMarker.isPresent()) {
					s.updateCall(bondsHasMarker, myMarker.get());
				}
				else {
					s.updateCall(bondsNoMarker, linestack);
				}
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(linestack.getAbility().getId()));
				BuffApplied spreadBuff = buffs.findBuffById(0xDDE);
				BuffApplied stackBuff = buffs.findBuffById(0xDDF);
				log.info("Daemoniac Bonds: Got Buffs");
				if (spreadBuff.getInitialDuration().compareTo(stackBuff.getInitialDuration()) > 0) {
					s.updateCall(bondsStackFirst, stackBuff);
					s.waitBuffRemoved(buffs, stackBuff);
					s.updateCall(bondsSpread, spreadBuff);
				}
				else {
					s.updateCall(bondsSpreadFirst, spreadBuff);
					s.waitBuffRemoved(buffs, spreadBuff);
					s.updateCall(bondsStack, stackBuff);
				}
				log.info("Daemoniac Bonds: Done");


			}

	);

}
