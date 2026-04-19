package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SqAction;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitUntilDurationAction<X extends BaseEvent> implements SqAction<X> {

	private static final Logger log = LoggerFactory.getLogger(WaitUntilDurationAction.class);

	@Description("Remaining Duration (ms)")
	public long remainingDurationMs = 1000;

	@Override
	public String fixedLabel() {
		return "Wait Until Remaining Duration Below";
	}

	@Override
	public String dynamicLabel() {
		return "Wait until remaining duration <= %sms".formatted(remainingDurationMs);
	}

	@Override
	public void accept(SequentialTriggerController<X> stc, EasyTriggerContext context, X rawEvent) {
		if (rawEvent instanceof HasDuration event) {
			long msToWait = event.getEstimatedRemainingDuration().minusMillis(remainingDurationMs).toMillis();
			if (msToWait > 0) {
				log.info("Waiting {} ms for duration to fall below {}", msToWait, remainingDurationMs);
				stc.waitMs(msToWait);
			}
		}
	}
}
