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
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@CalloutRepo(name = "P6S", duty = KnownDuty.P6S)
public class P6S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P6S.class);
	private final ModifiableCallout<AbilityCastStart> aethericPolyominoid = ModifiableCallout.durationBasedCall("Aetheric Polyominoid", "Tiles"); //????+2 tile explosion
	private final ModifiableCallout<AbilityCastStart> chelicSynergy = ModifiableCallout.durationBasedCall("Chelic Synergy", "Buster with Bleed"); //????+2 tile explosion
	private final ModifiableCallout<AbilityCastStart> unholyDarkness = ModifiableCallout.durationBasedCall("Unholy Darkness", "Healer Stacks"); //????+2 tile explosion
	private final ModifiableCallout<AbilityCastStart> exoCleaver = ModifiableCallout.durationBasedCall("Exocleaver", "Cleaves"); //????+2 tile explosion
	//	private final ModifiableCallout<AbilityCastStart> polyominoidSigma = ModifiableCallout.durationBasedCall("Polyominoid Sigma", "Tiles Swapping");
	private final ModifiableCallout<AbilityCastStart> chorosIxouSides = ModifiableCallout.durationBasedCall("Choros Ixou Sides Hit First", "Front/Back then Sides");
	private final ModifiableCallout<AbilityCastStart> chorosIxouFrontBack = ModifiableCallout.durationBasedCall("Choros Ixou Front Back Hit First", "Sides then Front/Back");
	private final ModifiableCallout<AbilityCastStart> hemitheosDarkIV = ModifiableCallout.durationBasedCall("Hemitheos's Dark IV", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> pathogenicCells = ModifiableCallout.durationBasedCall("Pathogenic Cells", "Check Number");
	private final ModifiableCallout<AbilityCastStart> aetherialExchange = ModifiableCallout.durationBasedCall("Aetherial Exchange", "Check Tether");
	private final ModifiableCallout<AbilityCastStart> synergy = ModifiableCallout.durationBasedCall("Synergy", "Tankbuster"); //????+1 on MT, ????+2 on OT
	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread"); //????+1 on MT, ????+2 on OT
	private final ModifiableCallout<AbilityCastStart> darkSphere = ModifiableCallout.durationBasedCall("Dark Sphere", "Spread to Safe Spots"); //????+1 on MT, ????+2 on OT
//	private final ModifiableCallout<AbilityCastStart> stropheIxouCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Clockwise");
//	private final ModifiableCallout<AbilityCastStart> stropheIxouCCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Counterclockwise");
//	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread"); //????-1 real boss
	private final ModifiableCallout<AbilityCastStart> darkPerimeter = ModifiableCallout.durationBasedCall("Dark Perimeter", "Donut on YOU");
	private final ModifiableCallout<AbilityCastStart> darkburst = ModifiableCallout.durationBasedCall("Darkburst", "AOE on YOU");
	private final ModifiableCallout<AbilityCastStart> unholyDarknessMarker = ModifiableCallout.durationBasedCall("Unholy Darkness Marker", "Stack on YOU");

//	private final ModifiableCallout<HasDuration> glossomorph = ModifiableCallout.durationBasedCall("Glossomorph debuff", "Point Away Soon").autoIcon();

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P6S(XivState state, StatusEffectRepository buffs) {
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
		return state.zoneIs(0x43C);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		// Unknown:
		/*
			_rsv_30858 (788A) - chelic synergy (buster)
			_rsv_30828 (786C) - ?

		*/
		switch (id) {
			case 0x7866 -> call = aethericPolyominoid;
			case 30858 -> call = chelicSynergy;
			case 0x7891 -> call = unholyDarkness;
			case 0x7869 -> call = exoCleaver;
			case 0x7864 -> call = pathogenicCells;
			case 0x784D -> call = aetherialExchange;
			case 0x7887 -> call = synergy;
			case 0x7881 -> call = chorosIxouFrontBack;
			case 0x7883 -> call = chorosIxouSides;
			case 0x788D -> call = darkAshes;
			case 0x788F -> call = darkSphere;
//		else if (id == 0x0)
//			call = polyominoidSigma;
//		else if (id == 0x0)
//			call = chorosIxouSides;
//		else if (id == 0x0)
//			call = chorosIxouFrontBack;
			case 0x7860 -> call = hemitheosDarkIV;
//		else if (id == 0x0) //see synergy declaration
//			call = synergy;
//		else if (id == 0x0)
//			call = stropheIxouCCW;
//		else if (id == 0x0)
//			call = stropheIxouCW;
//		else if (id == 0x0 && event.getTarget().isThePlayer())
//			call = darkAshes;
			default -> {
				return;
			}
		}
		if(event.getTarget().isThePlayer())
			switch (id) {
				case 0x7873 -> call = darkPerimeter;
				case 0x7870 -> call = darkburst;
				case 0x786E -> call = unholyDarknessMarker;
				default -> {
					return;
				}
			}
		context.accept(call.getModified(event));
	}

//	@HandleEvents
//	public void buffApplied(EventContext context, BuffApplied event) {
//		long id = event.getBuff().getId();
//		Duration duration = event.getInitialDuration();
//		ModifiableCallout<HasDuration> call;
//		if (event.getTarget().isThePlayer() && id == 0x0 && !event.isRefresh()) //???+8 bad glossomorph
//			call = glossomorph;
//		else
//			return;
//
//		context.accept(call.getModified(event));
//	}

	@HandleEvents
	public void headMarker(EventContext context, HeadMarkerEvent event) {
		long id = event.getMarkerId();
		log.info("Gained head marker with ID: {}", id);
		//TODO: flood ray number call
	}
}
