package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SqAction;
import gg.xp.xivsupport.events.triggers.marks.gui.AutoMarkGui;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitBuffDurationAction implements SqAction<BuffApplied> {

	private static final Logger log = LoggerFactory.getLogger(WaitBuffDurationAction.class);

	@JsonIgnore
	private final StatusEffectRepository buffs;

	public WaitBuffDurationAction(@JacksonInject(useInput = OptBoolean.FALSE) StatusEffectRepository buffs) {
		this.buffs = buffs;
	}

	@Description("Remaining Duration")
	public long remainingDurationMs = 1000;
	@Description("Stop Trigger if Buff Removed")
	public boolean stopIfGone;

	@Override
	public String fixedLabel() {
		return "Wait Until Buff Duration Below";
	}

	@Override
	public String dynamicLabel() {
		return "Wait until remaining cast duration <= %sms".formatted(remainingDurationMs);
	}

	@Override
	public void accept(SequentialTriggerController<BuffApplied> stc, EasyTriggerContext context, BuffApplied event) {
		while (true) {
			BuffApplied latest = buffs.getLatest(event);
			if (latest == null) {
				if (stopIfGone) {
					context.setStopProcessing(true);
				}
				return;
			}
			else {
				// TODO: this does not handle the case of a buff being replaced with one of a shorter duration
				long msToWait = latest.getEstimatedRemainingDuration().minusMillis(remainingDurationMs).toMillis();
				if (msToWait > 0) {
					stc.waitMs(msToWait);
				}
				else {
					return;
				}
			}
		}
	}

	@Override
	public void accept(EasyTriggerContext context, BuffApplied event) {
		// Handled above
	}
}
