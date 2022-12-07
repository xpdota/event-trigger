package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SqAction;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;

public class WaitAction implements SqAction<BaseEvent> {

	@Description("MS to wait")
	public long waitTimeMs = 1000;

	@Override
	public void accept(EasyTriggerContext context, BaseEvent event) {
		// TODO: this method is useless for these
	}

	@Override
	public String fixedLabel() {
		return "Wait";
	}

	@Override
	public String dynamicLabel() {
		return "Wait %sms".formatted(waitTimeMs);
	}

	@Override
	public void accept(SequentialTriggerController<BaseEvent> stc, EasyTriggerContext context, BaseEvent event) {
		stc.waitMs(waitTimeMs);
	}
}
