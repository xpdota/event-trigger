package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "P5", duty = KnownDuty.P5)
public class P5S {
	private static final Logger log = LoggerFactory.getLogger(P5S.class);
	private final ModifiableCallout<AbilityCastStart> RUbysomethingidk = ModifiableCallout.durationBasedCall("Rubysomething", "Raidwide");

	public P5S(XivState state) {
		this.state = state;
	}

	private final XivState state;

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0)
				call = RUbysomethingidk;
		}
	}
}
