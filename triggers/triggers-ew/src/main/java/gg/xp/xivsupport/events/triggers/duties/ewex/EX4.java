package gg.xp.xivsupport.events.triggers.duties.ewex;

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
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "EX4", duty = KnownDuty.BarbarEx)
public class EX4 extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(EX4.class);

	private final ModifiableCallout<AbilityCastStart> voidAero3 = ModifiableCallout.durationBasedCall("Void Aero III", "Tankbuster");
	private final ModifiableCallout<AbilityCastStart> voidAero4 = ModifiableCallout.durationBasedCall("Void Aero IV", "Raidwide");
	// TODO: spread on its own, or after another mech
	private final ModifiableCallout<AbilityCastStart> hairSpray = ModifiableCallout.durationBasedCall("Hair Spray", "Spread");
	// TODO: add direction to this
	private final ModifiableCallout<AbilityCastStart> savageBarberyIn = ModifiableCallout.durationBasedCall("Savage Barbery (In)", "In");
	// TODO: add direction to this
	private final ModifiableCallout<AbilityCastStart> savageBarberyOut = ModifiableCallout.durationBasedCall("Savage Barbery (Cleave)", "Away");
	private final ModifiableCallout<AbilityCastStart> deadlyTwist = ModifiableCallout.durationBasedCall("Deadly Twist", "Light Parties");
	private final ModifiableCallout<AbilityCastStart> hairRaid = ModifiableCallout.durationBasedCall("Hair Raid", "In");
	private final ModifiableCallout<AbilityCastStart> hairRaidWall = ModifiableCallout.durationBasedCall("Hair Raid (Wall)", "{dir} safe");
	private final ModifiableCallout<AbilityCastStart> secretBreeze = ModifiableCallout.durationBasedCall("Secret Breeze", "Protean, Repeat");
	private final ModifiableCallout<AbilityCastStart> impact = ModifiableCallout.durationBasedCall("Impact", "Knockback");
	private final ModifiableCallout<HeadMarkerEvent> brittleBoulder = new ModifiableCallout<>("Brittle Boulder", "Bait middle then out");
	private final ModifiableCallout<TetherEvent> brutalRush = new ModifiableCallout<>("Brutal Rush", "Tether on you");
	private final ModifiableCallout<HeadMarkerEvent> boldBoulder = new ModifiableCallout<>("Bold Boulder", "Flare");
	private final ModifiableCallout<HeadMarkerEvent> notBoldBoulder = new ModifiableCallout<>("Not Bold Boulder", "Stack");
	private final ModifiableCallout<HeadMarkerEvent> circle = new ModifiableCallout<>("Circle");
	private final ModifiableCallout<HeadMarkerEvent> triangle = new ModifiableCallout<>("Triangle");
	private final ModifiableCallout<HeadMarkerEvent> square = new ModifiableCallout<>("Square");
	private final ModifiableCallout<HeadMarkerEvent> cross = new ModifiableCallout<>("Cross");

	private final ModifiableCallout<AbilityCastStart> teasingTanglesSpread = ModifiableCallout.durationBasedCall("Teasing Tangles: Spread", "Spread");
	private final ModifiableCallout<AbilityCastStart> teasingTanglesEnum = ModifiableCallout.durationBasedCall("Teasing Tangles: Enum on You", "Stack with Partner");
	private final ModifiableCallout<AbilityCastStart> teasingTanglesNothing = ModifiableCallout.durationBasedCall("Teasing Tangles: Nothing", "Stack with Partner");

	// No other (good) way to suppress duplicates since they're both cast by fakes
	private final RepeatSuppressor lightPartySupp = new RepeatSuppressor(Duration.ofSeconds(1));

	private final XivState state;

	public EX4(XivState state) {
		this.state = state;
	}

	public XivState getState() {
		return state;
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
			case 0x75A0 -> call = impact;
//			case 0x759E -> {
//				// Brittle Boulder, Bait Middle then out
//				if (acs.getTarget().isThePlayer()) {
//					call = brittleBoulder;
//				}
//			}
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

	//	@AutoFeed
//	private final SequentialTrigger<BaseEvent> savageBarberySeq = new SequentialTrigger<>(5000, BaseEvent.class,
//			e -> e instanceof AbilityCastStart acs && acs.abilityIdMatches())
//
	@HandleEvents
	public void headmarker(EventContext context, HeadMarkerEvent hme) {
		if (hme.getTarget().isThePlayer()) {
			ModifiableCallout<HeadMarkerEvent> call;
			switch ((int) hme.getMarkerId()) {
				case 0x16D -> call = brittleBoulder; // Big headmarker, bait middle then out
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

	@AutoFeed
	private final SequentialTrigger<HeadMarkerEvent> twoFlareAndStack = new SequentialTrigger<>(5_000, HeadMarkerEvent.class,
			hme -> hme.getMarkerId() == 0x15A,
			(e1, s) -> {
				HeadMarkerEvent e2 = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getMarkerId() == 0x15A);
				if (e1.getTarget().isThePlayer()) {
					s.updateCall(boldBoulder.getModified(e1));
				}
				else if (e2.getTarget().isThePlayer()) {
					s.updateCall(boldBoulder.getModified(e2));
				}
				else {
					s.updateCall(notBoldBoulder.getModified(e1));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> teasingTangles = new SequentialTrigger<>(30_000, BaseEvent.class,
			// Start on "Teasing Tangles"
			e -> e instanceof AbilityCastStart acs && acs.abilityIdMatches(0x75A9),
			(e1, s) -> {
				// 75A8 is Upbraid (partner enum)
				// 7413 is Hair Flay (spread)
				EventCollector<AbilityCastStart> upbraid = new EventCollector<>(acs -> acs.abilityIdMatches(0x75a8));
				EventCollector<AbilityCastStart> hairFlay = new EventCollector<>(acs -> acs.abilityIdMatches(0x7413));
				log.info("Teasing Tangles: Start");

				s.collectEvents(6, 10_000, AbilityCastStart.class, true, List.of(upbraid, hairFlay));
				log.info("Teasing Tangles: Upbraid on {}", upbraid.getEvents().stream().map(HasTargetEntity::getTarget).toList());
				log.info("Teasing Tangles: Hair Flay on {}", hairFlay.getEvents().stream().map(HasTargetEntity::getTarget).toList());
				hairFlay.findAny(acs -> acs.getTarget().isThePlayer())
						.ifPresentOrElse(
								acs -> s.updateCall(teasingTanglesSpread.getModified(acs)),
								() -> upbraid.findAny(acs -> acs.getTarget().isThePlayer())
										.ifPresentOrElse(
												acs -> s.updateCall(teasingTanglesEnum.getModified(acs)),
												() -> s.updateCall(teasingTanglesNothing.getModified(hairFlay.getEvents().get(0)))
										));
			});
}
