package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "P10S", duty = KnownDuty.P10S)
public class P10S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P10S.class);

	private final XivState state;
	private final StatusEffectRepository buffs;

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

}
