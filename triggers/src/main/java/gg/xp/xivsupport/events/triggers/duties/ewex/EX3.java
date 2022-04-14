package gg.xp.xivsupport.events.triggers.duties.ewex;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.Map;

@CalloutRepo("Endsinger Extreme")
public class EX3 implements FilteredEventHandler {

	// TODO: all the complex mechanics, and the party stack

	private final ModifiableCallout<AbilityCastStart> elegeia = ModifiableCallout.durationBasedCall("Elegeia", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> telos = ModifiableCallout.durationBasedCall("Telos", "Big Raidwide");
	// TODO: what is the tell for this?
	private final ModifiableCallout<AbilityCastStart> telomania = ModifiableCallout.durationBasedCall("Telomania", "Raidwides and Bleed");
	private final ModifiableCallout<AbilityCastStart> elenchosSides = ModifiableCallout.durationBasedCall("Elenchos (Sides)", "Sides");
	private final ModifiableCallout<AbilityCastStart> elenchosMiddle = ModifiableCallout.durationBasedCall("Elenchos (Middle)", "Middle");
	private final ModifiableCallout<AbilityCastStart> hubris = ModifiableCallout.durationBasedCall("Hubris", "Tankbuster");

	private final ModifiableCallout<AbilityCastStart> blueStar = ModifiableCallout.durationBasedCall("Blue Star", "Knockback from {starDir}");
	private final ModifiableCallout<AbilityCastStart> redStar = ModifiableCallout.durationBasedCall("Red Star", "Away from {starDir}");

	private final ModifiableCallout<TetherEvent> tetherCall = new ModifiableCallout<>("Tether Break", "Break Tether (with {otherTarget})");

//	private final ModifiableCallout<HeadMarkerEvent> donut = new ModifiableCallout<>("Donut Marker", "Donut");
//	private final ModifiableCallout<HeadMarkerEvent> stack = new ModifiableCallout<>("Stack Marker", "Stack");
//	private final ModifiableCallout<HeadMarkerEvent> flare = new ModifiableCallout<>("Flare Marker", "Flare");
//	private final ModifiableCallout<HeadMarkerEvent> spread = new ModifiableCallout<>("Spread Marker", "Spread");

	private final ModifiableCallout<BuffApplied> donut = ModifiableCallout.durationBasedCall("Donut Marker", "Donut");
	private final ModifiableCallout<BuffApplied> stack = ModifiableCallout.durationBasedCall("Stack Marker", "Stack");
	private final ModifiableCallout<BuffApplied> flare = ModifiableCallout.durationBasedCall("Flare Marker", "Flare");
	private final ModifiableCallout<BuffApplied> spread = ModifiableCallout.durationBasedCall("Spread Marker", "Spread");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	private final XivState state;

	public EX3(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x3e6);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		ModifiableCallout<AbilityCastStart> call;
		if (event.getSource().getType() == CombatantType.NPC) {
			int id = (int) event.getAbility().getId();
			call = switch (id) {
				case 0x6FF6 -> elegeia;
				case 0x702E -> telos;
				case 0x7020 -> elenchosMiddle;
				case 0x7022 -> elenchosSides;
				case 0x702C -> hubris;
				default -> null;
			};
			if (call != null) {
				context.accept(call.getModified(event));
			}
		}
	}

	@HandleEvents
	public void starCollisions(EventContext context, AbilityCastStart event) {
		ModifiableCallout<AbilityCastStart> call;
		if (event.getSource().getType() == CombatantType.NPC || event.getSource().getbNpcId() == 9020) {
			int id = (int) event.getAbility().getId();
			call = switch (id) {
				case 0x6FFA, 0x7003 -> redStar;
				case 0x6FFB, 0x7005 -> blueStar;
				default -> null;
			};
			if (call != null) {
				context.accept(call.getModified(event, Map.of("starDir", arenaPos.forCombatant(event.getSource()))));
			}
		}
	}

	@HandleEvents
	public void tether(EventContext context, TetherEvent event) {
		if (event.getId() == 0xA3) {
			XivCombatant thePlayer = event.getTargetMatching(XivCombatant::isThePlayer);
			if (thePlayer != null) {
				XivCombatant otherTarget = event.getTargetMatching(cbt -> !cbt.isThePlayer());
				context.accept(tetherCall.getModified(event, Map.of("otherTarget", otherTarget == null ? "Error" : otherTarget)));
			}
		}
	}

//	private Long firstHeadmark;
//
//	private int getHeadmarkOffset(HeadMarkerEvent event) {
//		if (firstHeadmark == null) {
//			firstHeadmark = event.getMarkerId();
//		}
//		return (int) (event.getMarkerId() - firstHeadmark);
//	}
//

	@HandleEvents
	public void buffs(EventContext context, BuffApplied event) {
		// This is done unconditionally to create the headmarker offset
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}

		// Headmarkers:
		/*
			326 (0) - Tether precursor
			344 (+18) - Tank buster
			161 (-165) - Healer Stacks

			328 (+2) - Something? All 8 people got it.

			318 (-8) - Stack?
			322 (-4) - Donut?
			327 (+1) - Flare?
			328 (+2) - Spread?

			325 (-1) - ?
			221 (-105) - ?
			323 (-3) - ?
			324 (-2) - ?

		 */
		int buffId = (int) event.getBuff().getId();

		ModifiableCallout<BuffApplied> call =
				switch (buffId) {
					case 0xBAD -> donut;
					case 0xBAE -> spread;
					case 0xBAF -> flare;
					case 0xBB0 -> stack;
					default -> null;
				};
		if (call != null) {
			context.accept(call.getModified(event));
		}
	}

}
