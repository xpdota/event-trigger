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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@CalloutRepo(name = "P6S", duty = KnownDuty.P6S)
public class P6S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P6S.class);

	/*
		TODO:
		Call out current LC number
		Point in/Point out (status 0xCF2 is face out, 0xCF3 is face in)
		3rd set of healer stacks + other mechanics
		Whatever mechs come after

		Probably not doable:
		Floor tile patterns (likely need MapEvent)


	 */

	private final ModifiableCallout<AbilityCastStart> aethericPolyominoid = ModifiableCallout.durationBasedCall("Aetheric Polyominoid", "Tiles"); //????+2 tile explosion
	private final ModifiableCallout<AbilityCastStart> chelicSynergy = ModifiableCallout.durationBasedCall("Chelic Synergy", "Buster with Bleed"); //????+2 tile explosion
	private final ModifiableCallout<AbilityCastStart> unholyDarknessHealer = ModifiableCallout.durationBasedCall("Unholy Darkness", "Healer Stacks"); //????+2 tile explosion
	private final ModifiableCallout<AbilityCastStart> exoCleaver = ModifiableCallout.durationBasedCall("Exocleaver", "Cleaves"); //????+2 tile explosion
	//	private final ModifiableCallout<AbilityCastStart> polyominoidSigma = ModifiableCallout.durationBasedCall("Polyominoid Sigma", "Tiles Swapping");
	private final ModifiableCallout<HeadMarkerEvent> limitCutNumber = new ModifiableCallout<>("Limit Cut Number", "{number}", 20_000);
	private final ModifiableCallout<AbilityCastStart> chorosIxouSides = ModifiableCallout.durationBasedCall("Choros Ixou Sides Hit First", "Front/Back then Sides");
	private final ModifiableCallout<AbilityCastStart> chorosIxouFrontBack = ModifiableCallout.durationBasedCall("Choros Ixou Front Back Hit First", "Sides then Front/Back");
	private final ModifiableCallout<AbilityCastStart> hemitheosDarkIV = ModifiableCallout.durationBasedCall("Hemitheos's Dark IV", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> aetherialExchange = ModifiableCallout.durationBasedCall("Aetherial Exchange", "Check Tether");
	private final ModifiableCallout<AbilityCastStart> synergy = ModifiableCallout.durationBasedCall("Synergy", "Tankbuster"); //????+1 on MT, ????+2 on OT
	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread");
	private final ModifiableCallout<AbilityCastStart> darkSphere = ModifiableCallout.durationBasedCall("Dark Sphere", "Spread to Safe Spots");
	private final ModifiableCallout<AbilityCastStart> darkBurst = ModifiableCallout.durationBasedCall("Dark Burst (Flare)", "Out");
	private final ModifiableCallout<AbilityCastStart> darkPerimeter = ModifiableCallout.durationBasedCall("Dark Sphere (Donut)", "Stack in Middle");
	private final ModifiableCallout<AbilityCastStart> unholyDarkness2 = ModifiableCallout.durationBasedCall("Unholy Darkness (Single)", "Stack in Middle");
	private final ModifiableCallout<AbilityCastStart> darkDomeBait = ModifiableCallout.durationBasedCall("Dark Dome Bait", "Bait");
	private final ModifiableCallout<AbilityCastStart> darkDomeMove = ModifiableCallout.durationBasedCall("Dark Dome Move", "Move!");
//	private final ModifiableCallout<AbilityCastStart> stropheIxouCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Clockwise");
//	private final ModifiableCallout<AbilityCastStart> stropheIxouCCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Counterclockwise");
//	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread"); //????-1 real boss

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
		ModifiableCallout<AbilityCastStart> call = null;
		// Unknown:
		/*
			_rsv_30858 (788A) - chelic synergy (buster)
			_rsv_30828 (786C) - ?

		*/
		switch (id) {
			case 0x7866 -> call = aethericPolyominoid;
			case 30858 -> call = chelicSynergy;
			case 0x7891 -> call = unholyDarknessHealer;
			case 0x7869 -> call = exoCleaver;
			case 0x784D -> call = aetherialExchange;
			case 0x7887 -> call = synergy;
			case 0x7881 -> call = chorosIxouFrontBack;
			case 0x7883 -> call = chorosIxouSides;
			case 0x788D -> call = darkAshes;
			case 0x788F -> call = darkSphere;
			// Each of these seems to have 3 IDs. I'm guessing it's no-swap plus the two possibilities for swap
			case 0x7872, 0x7871, 0x7870 -> {
				if (event.getTarget().isThePlayer()) {
					call = darkBurst;
				}
			}
			case 0x7875, 0x7874, 0x7873 -> {
				if (event.getTarget().isThePlayer()) {
					call = darkPerimeter;
				}
			}
			case 0x786D, 0x786E, 0x786F -> {
				if (event.getTarget().isThePlayer()) {
					call = unholyDarkness2;
				}
			}
//			case 0x7871 -> call = unholyDarkness3;
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
		if (call != null) {
			context.accept(call.getModified(event));
		}
	}


	private Long firstHeadmark;

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		firstHeadmark = null;
	}

	@HandleEvents
	public void headmark(EventContext context, HeadMarkerEvent event) {
		int offset = getHeadmarkOffset(event);
		if (offset >= -239 && offset <= -232 && event.getTarget().isThePlayer()) {
			context.accept(limitCutNumber.getModified(event, Map.of("number", offset + 240)));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> darkDomeSq = new SequentialTrigger<>(10_000, BaseEvent.class,
			e1 -> e1 instanceof AbilityCastStart acs && acs.abilityIdMatches(30859),
			(e1, s) -> {
				log.info("Dark Dome: Start");
				s.updateCall(darkDomeBait.getModified((AbilityCastStart) e1));
				AbilityCastStart e2 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x788C));
				s.updateCall(darkDomeMove.getModified(e2));
				log.info("Dark Dome: End");
			});

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
}
