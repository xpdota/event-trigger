package gg.xp.xivsupport.events.triggers.duties.ewex;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.Duration;
import java.util.Map;

public class EX4 implements FilteredEventHandler {

	private final ModifiableCallout<AbilityCastStart> voidAero3 = ModifiableCallout.durationBasedCall("Void Aero III", "Tankbuster");
	private final ModifiableCallout<AbilityCastStart> voidAero4 = ModifiableCallout.durationBasedCall("Void Aero IV", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> hairSpray = ModifiableCallout.durationBasedCall("Hair Spray", "In/Wall then Spread");
	private final ModifiableCallout<AbilityCastStart> savageBarberyIn = ModifiableCallout.durationBasedCall("Savage Barbery (In)", "In");
	private final ModifiableCallout<AbilityCastStart> savageBarberyOut = ModifiableCallout.durationBasedCall("Savage Barbery (Out)", "Out");
	private final ModifiableCallout<AbilityCastStart> deadlyTwist = ModifiableCallout.durationBasedCall("Deadly Twist", "Light Parties");
	private final ModifiableCallout<AbilityCastStart> hairRaid = ModifiableCallout.durationBasedCall("Hair Raid", "In");
	private final ModifiableCallout<AbilityCastStart> hairRaidWall = ModifiableCallout.durationBasedCall("Hair Raid (Wall)", "{dir} safe");
	private final ModifiableCallout<AbilityCastStart> secretBreeze = ModifiableCallout.durationBasedCall("Secret Breeze", "Protean");
	private final ModifiableCallout<AbilityCastStart> brittleBoulder = ModifiableCallout.durationBasedCall("Brittle Boulder", "Bait middle then out");
	private final ModifiableCallout<TetherEvent> brutalRush = new ModifiableCallout<>("Brutal Rush", "Tether on you");
	private final ModifiableCallout<HeadMarkerEvent> boldBoulder = new ModifiableCallout<>("Bold Boulder", "Flare");
	private final ModifiableCallout<HeadMarkerEvent> circle = new ModifiableCallout<>("Circle");
	private final ModifiableCallout<HeadMarkerEvent> triangle = new ModifiableCallout<>("Triangle");
	private final ModifiableCallout<HeadMarkerEvent> square = new ModifiableCallout<>("Square");
	private final ModifiableCallout<HeadMarkerEvent> cross = new ModifiableCallout<>("Cross");

	// No other (good) way to suppress duplicates since they're both cast by fakes
	private final RepeatSuppressor lightPartySupp = new RepeatSuppressor(Duration.ofSeconds(1));

	private final XivState state;

	public EX4(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(1072);
	}

	@HandleEvents
	public void basicCasts(EventContext context, AbilityCastStart acs) {
		int id = (int) acs.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call = null;
		switch (id) {
			case 0x7571 -> call = voidAero3; // Void Aero III, Tankbuster
			case 0x7570 -> call = voidAero4; // Void Aero IV, raidwide
			case 0x75A6 -> {
				// Hair Spray, Move then Spread (must be targeted on you)
				if (acs.getTarget().isThePlayer()) {
					call = hairSpray;
				}
			}
			case 0x7574 -> call = savageBarberyIn; // Savage Barbery, In
			case 0x757A -> call = savageBarberyOut; // Savage Barbery, Out
			case 0x75A7 -> {
				// Deadly twist, Light Parties
				if (lightPartySupp.check(acs)) {
					call = deadlyTwist;
				}
			}
			case 0x757F -> call = hairRaid; // Hair Raid
			case 0x757C -> {
				// Hair raid directional
				ArenaSector bossFacing = ArenaPos.combatantFacing(acs.getSource());
				context.accept(hairRaidWall.getModified(acs, Map.of("dir", bossFacing)));
			}
			case 0x7580 -> call = secretBreeze; // Secret Breeze, Protean
			case 0x759E -> {
				// Brittle Boulder, Bait Middle then out
				if (acs.getTarget().isThePlayer()) {
					call = brittleBoulder;
				}
			}
		}
		if (call != null) {
			context.accept(call.getModified(acs));
		}
	}

	@HandleEvents
	public void tether(EventContext context, TetherEvent tether) {
		if (tether.getId() == 0x11 && tether.eitherTargetMatches(XivCombatant::isThePlayer)) {
			// Brutal Rush, Tether
			context.accept(brutalRush.getModified(tether));

		}
	}

	@HandleEvents
	public void headmarker(EventContext context, HeadMarkerEvent hme) {
		if (hme.getTarget().isThePlayer()) {
			ModifiableCallout<HeadMarkerEvent> call;
			switch ((int) hme.getMarkerId()) {
				case 0x15A -> call = boldBoulder; // Bold Boulder, Flare
				case 0x16F -> call = circle; // Circle
				case 0x170 -> call = triangle; // Triangle
				case 0x171 -> call = square; // Square
				case 0x172 -> call = cross; // Cross
				default -> {
					return;
				}
			}
			context.accept(call.getModified(hme));
		}
	}
}
