package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SqAction;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitCastDurationAction implements SqAction<AbilityCastStart> {

	private static final Logger log = LoggerFactory.getLogger(WaitCastDurationAction.class);

	@Description("Remaining Duration")
	public long remainingDurationMs = 1000;

	@Override
	public String fixedLabel() {
		return "Wait Until Cast Duration Below";
	}

	@Override
	public String dynamicLabel() {
		return "Wait until remaining cast duration <= %sms".formatted(remainingDurationMs);
	}

	@Override
	public void accept(SequentialTriggerController<AbilityCastStart> stc, EasyTriggerContext context, AbilityCastStart event) {
		long msToWait = event.getEstimatedRemainingDuration().minusMillis(remainingDurationMs).toMillis();
		if (msToWait > 0) {
			stc.waitMs(msToWait);
		}
	}

	@Override
	public void accept(EasyTriggerContext context, AbilityCastStart event) {
		// Handled above
	}
}
