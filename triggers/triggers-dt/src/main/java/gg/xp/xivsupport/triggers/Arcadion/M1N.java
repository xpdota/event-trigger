package gg.xp.xivsupport.triggers.Arcadion;

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
import gg.xp.xivsupport.events.actlines.events.SnapshotLocationDataEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@CalloutRepo(name = "M1N", duty = KnownDuty.M1N)
public class M1N extends AutoChildEventHandler implements FilteredEventHandler {
	public static final Logger log = LoggerFactory.getLogger(M1N.class);

	@NpcCastCallout(0x9309)
	private final ModifiableCallout<AbilityCastStart> oneTwoPawEastWest = ModifiableCallout.durationBasedCall("One-Two Paw: West safe", "West then East");
	@NpcCastCallout(0x930C)
	private final ModifiableCallout<AbilityCastStart> oneTwoPawWestEast = ModifiableCallout.durationBasedCall("One-Two Paw: East safe", "East then West");
	@NpcCastCallout(0x0)
	private final ModifiableCallout<AbilityCastStart> blackCatCrossingCardInter = ModifiableCallout.durationBasedCall("Black Cat Crossing: Inter safe", "Intercardinals then cardinals");
	@NpcCastCallout(0x930F)
	private final ModifiableCallout<AbilityCastStart> blackCatCrossingInterCard = ModifiableCallout.durationBasedCall("Black Cat Crossing: Card safe", "Cardinals then intercardinals");
	@NpcCastCallout({0x9321, 0x931F})
	private final ModifiableCallout<AbilityCastStart> leapingOneTwoPawEastWest = ModifiableCallout.durationBasedCall("Leaping One-Two Paw: West safe", "West then East");
	@NpcCastCallout({0x9320, 0x9322}) //dont know what the difference is (maybe leap direction?)
	private final ModifiableCallout<AbilityCastStart> leapingOneTwoPawWestEast = ModifiableCallout.durationBasedCall("Leaping One-Two Paw: East safe", "East then West");
	@NpcCastCallout(0x9810)
	private final ModifiableCallout<AbilityCastStart> leapingBlackCatCrossingCardInter = ModifiableCallout.durationBasedCall("Leaping Black Cat Crossing: Inter safe", "Inter then Cardinal");
	@NpcCastCallout(0x9329)
	private final ModifiableCallout<AbilityCastStart> leapingBlackCatCrossingInterCard = ModifiableCallout.durationBasedCall("Leaping Black Cat Crossing: Card safe", "Cardinal then inter");

	public M1N(XivState state) {
		this.state = state;
	}
	private final XivState state;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M1N);
	}

	private final ModifiableCallout<?> startSW = new ModifiableCallout<>("Mouser: Start SW", "Start South West");
	private final ModifiableCallout<?> startSE = new ModifiableCallout<>("Mouser: Start SE", "Start South East");
	private final ModifiableCallout<?> swSafe = new ModifiableCallout<>("Mouser: SW", "South West");
	private final ModifiableCallout<?> seSafe = new ModifiableCallout<>("Mouser: SE", "South East");

	private boolean isSouthernTile(Position pos) {
		return pos.getY() > 100 //below the center horizontal
		       && pos.getX() < 106 //clamps to middle two tiles
		       && pos.getX() > 94;
	}
	private boolean isWestTile(Position pos) {
		return pos.getX() < 100;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> mouser = SqtTemplates.sq(20_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x9313),
			(e1, s) -> {
				log.info("Mouser: Start");
				//check both damage hits, plus the shatter hit. may be double or single movement
				List<AbilityUsedEvent> hits = s.waitEvents(3, AbilityUsedEvent.class, aue ->
						aue.abilityIdMatches(0x9315, 0x996B)
						&& isSouthernTile(aue.getTarget().getPos()));

				Position hit1Pos = hits.get(0).getTarget().getPos();
				Position hit2Pos = hits.get(1).getTarget().getPos();
				Position hit3Pos = hits.get(2).getTarget().getPos();

				//which tile was most recently hit
				boolean wasWest = true;
				//Starting position
				if(isWestTile(hit1Pos))
					s.updateCall(startSE);
				else {
					s.updateCall(startSW);
					wasWest = false;
				}

				//Wait for each southern mouser cast
				//hit 1
				s.waitEvent(SnapshotLocationDataEvent.class, slde ->
						slde.originalEvent().abilityIdMatches(0x9316, 0x94A5)
						&& isSouthernTile(slde.getPos()));
				//Call new if its different
				if(isWestTile(hit2Pos) && !wasWest)
					s.updateCall(seSafe);
				else if(!isWestTile(hit2Pos) && wasWest)
					s.updateCall(swSafe);

				wasWest = isWestTile(hit2Pos);

				//hit 2
				s.waitEvent(SnapshotLocationDataEvent.class, slde ->
						slde.originalEvent().abilityIdMatches(0x9316, 0x94A5)
						&& isSouthernTile(slde.getPos()));
				//Call new if its different
				if(isWestTile(hit3Pos) && !wasWest)
					s.updateCall(seSafe);
				else if(!isWestTile(hit3Pos) && wasWest)
					s.updateCall(swSafe);

				//hit 3, dont need to move again
			});
}
