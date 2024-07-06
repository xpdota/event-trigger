package gg.xp.xivsupport.triggers.dtex;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;

@CalloutRepo(name = "EX2", duty = KnownDuty.DtEx2)
public class DTEx2 extends AutoChildEventHandler implements FilteredEventHandler {
	private final XivState state;

	public DTEx2(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.DtEx2);
	}

	@NpcCastCallout(0x9398)
	private final ModifiableCallout<AbilityCastStart> actualize = ModifiableCallout.durationBasedCall("Actualize", "Raidwide");

	@NpcCastCallout(0x93a2)
	private final ModifiableCallout<AbilityCastStart> multidirectionalDivie = ModifiableCallout.durationBasedCall("Multidirectional Divide", "Cross");

	@NpcCastCallout(0x993b)
	private final ModifiableCallout<AbilityCastStart> regicidalRage = ModifiableCallout.durationBasedCall("Regicidal Rage", "Tank Tethers");

	@PlayerHeadmarker(value = 0, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> yellowMarker = new ModifiableCallout<>("Yellow Marker", "Spread on Tiles");

	@NpcCastCallout(0x9397)
	private final ModifiableCallout<AbilityCastStart> dawnOfAnAge = ModifiableCallout.durationBasedCall("Dawn of an Age", "Raidwide");

	@PlayerStatusCallout(0x301)
	private final ModifiableCallout<BuffApplied> burningChains = new ModifiableCallout<>("Burning Chains", "Break Chains");

	@NpcCastCallout(0x938a)
	private final ModifiableCallout<AbilityCastStart> projectionOfTriumph = ModifiableCallout.durationBasedCall("Projection of Triumph", "Avoid Balls, Follow Donuts");

	@NpcCastCallout(0x9a88)
	private final ModifiableCallout<AbilityCastStart> projectionOfTurmoil = ModifiableCallout.durationBasedCall("Projection of Turmoil", "Take Stacks Sequentially");


}
