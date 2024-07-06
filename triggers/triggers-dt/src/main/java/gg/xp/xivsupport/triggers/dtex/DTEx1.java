package gg.xp.xivsupport.triggers.dtex;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;

@CalloutRepo(name = "EX1", duty = KnownDuty.DtEx1)
public class DTEx1 extends AutoChildEventHandler implements FilteredEventHandler {
	private final XivState state;

	public DTEx1(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.DtEx1);
	}


	@NpcCastCallout(0x9008)
	private final ModifiableCallout<AbilityCastStart> disasterZone = ModifiableCallout.durationBasedCall("Disaster Zone", "Raidwide");

	// TODO: this might be the fake ID. Find real ID.
	@NpcCastCallout(0x8fcc)
	private final ModifiableCallout<AbilityCastStart> actualize = ModifiableCallout.durationBasedCall("Sliterhing Strike", "Out");

	@NpcCastCallout(0x9008)
	private final ModifiableCallout<AbilityCastStart> tulidisaster = ModifiableCallout.durationBasedCall("Tulidisaster", "Multiple Raidwides");



}
