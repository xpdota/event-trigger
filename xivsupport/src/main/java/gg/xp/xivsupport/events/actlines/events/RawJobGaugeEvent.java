package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.util.Arrays;

// TODO: mark this as system event again
@SystemEvent
public class RawJobGaugeEvent extends BaseEvent implements HasTargetEntity, HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = 6440552025070849281L;
	private final XivCombatant target;
	private final Job job;
	private final byte[] out;

	public RawJobGaugeEvent(XivCombatant target, Job job, byte[] out) {
		this.target = target;
		this.job = job;
		this.out = out;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public Job getJob() {
		return job;
	}

	public byte[] getRawData() {
		return Arrays.copyOf(out, out.length);
	}


	@Override
	public String getPrimaryValue() {
		return String.format("%s: %s", job, Arrays.toString(out));
	}
}
