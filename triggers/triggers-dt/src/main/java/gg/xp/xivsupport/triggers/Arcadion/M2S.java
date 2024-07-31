package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;

@CalloutRepo(name = "M2S", duty = KnownDuty.M2S)
public class M2S extends AutoChildEventHandler implements FilteredEventHandler {
	public M2S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private XivState state;
	private StatusEffectRepository buffs;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M2S);
	}
//
//	@NpcCastCallout()
//	private final ModifiableCallout<AbilityCastStart> raidwide = ModifiableCallout.durationBasedCall("Something", "Raidwide");
//	@NpcCastCallout()
//	private final ModifiableCallout<AbilityCastStart> tankstack = ModifiableCallout.durationBasedCall("Something", "Tank Stack");
//	@NpcCastCallout(value = 0xTODO, suppressMs = 100)
//	private final ModifiableCallout<AbilityCastStart> killerSting = ModifiableCallout.durationBasedCall("Killer Sting", "Tank Cleaves");
//
//	private final SequentialTrigger<BaseEvent>
}
