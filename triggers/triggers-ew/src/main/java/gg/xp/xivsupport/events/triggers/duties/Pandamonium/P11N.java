package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "P11N", duty = KnownDuty.None)
public class P11N extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P11N.class);

	private final XivState state;
	private final StatusEffectRepository buffs;

	public P11N(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return false;
//		return state.dutyIs(KnownDuty.P11N);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}
}
