package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@CalloutRepo(name = "P6N", duty = KnownDuty.P6N)
public class P6N extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P6N.class);
	private final ModifiableCallout<AbilityCastStart> aethericPolyominoid = ModifiableCallout.durationBasedCall("Aetheric Polyominoid", "Tiles"); //7855 tile explosion
	private final ModifiableCallout<AbilityCastStart> polyominoidSigma = ModifiableCallout.durationBasedCall("Polyominoid Sigma", "Tiles Swapping");
	private final ModifiableCallout<AbilityCastStart> chorosIxouSides = ModifiableCallout.durationBasedCall("Choros Ixou Sides hit", "Go Front/Back");
	private final ModifiableCallout<AbilityCastStart> chorosIxouFrontBack = ModifiableCallout.durationBasedCall("Choros Ixou Front Back hit", "Go Sides");
	private final ModifiableCallout<AbilityCastStart> hemitheosDarkIV = ModifiableCallout.durationBasedCall("Hemitheos's Dark IV", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> synergy = ModifiableCallout.durationBasedCall("Synergy", "Tankbuster"); //785C on MT, 785D on OT
	private final ModifiableCallout<AbilityCastStart> stropheIxouCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Clockwise");
	private final ModifiableCallout<AbilityCastStart> stropheIxouCCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Counterclockwise");
	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread"); //785E real boss

	private final ModifiableCallout<HasDuration> glossomorph = ModifiableCallout.durationBasedCall("Glossomorph debuff", "Point Away Soon").autoIcon();

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P6N(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	private final StatusEffectRepository buffs;
	private StatusEffectRepository getBuffs() {
		return this.buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x43B);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		if (id == 0x7853)
			call = aethericPolyominoid;
		else if (id == 0x7856)
			call = polyominoidSigma;
		else if (id == 0x7858)
			call = chorosIxouSides;
		else if (id == 0x7857)
			call = chorosIxouFrontBack;
		else if (id == 0x784E)
			call = hemitheosDarkIV;
		else if (id == 0x785B) //see synergy declaration
			call = synergy;
		else if (id == 0x7A11)
			call = stropheIxouCCW;
		else if (id == 0x7A12)
			call = stropheIxouCW;
		else if (id == 0x785F && event.getTarget().isThePlayer())
			call = darkAshes;
		else
			return;

		context.accept(call.getModified(event));
	}

	@HandleEvents
	public void buffApplied(EventContext context, BuffApplied event) {
		long id = event.getBuff().getId();
		Duration duration = event.getInitialDuration();
		ModifiableCallout<HasDuration> call;
		if (event.getTarget().isThePlayer() && id == 0xCF2 && !event.isRefresh()) //CFA bad glossomorph
			call = glossomorph;
		else
			return;

		context.accept(call.getModified(event));
	}
}
