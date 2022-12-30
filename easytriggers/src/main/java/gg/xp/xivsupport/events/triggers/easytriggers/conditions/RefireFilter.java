package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class RefireFilter implements SimpleCondition<Event> {

	@Description("Refire Period (ms)")
	public long refireMs = 1_000;

	@JsonIgnore
	private long last;

	@Override
	public String fixedLabel() {
		return "Refire Suppression";
	}

	@Override
	public String dynamicLabel() {
		return "Refire period of %sms".formatted(refireMs);
	}

	@Override
	public boolean test(Event event) {
		long thisEventAt = event.getEffectiveHappenedAt().toEpochMilli();
		boolean result = last + refireMs <= thisEventAt;
		if (result) {
			last = thisEventAt;
		}
		return result;
	}

	// This one should be last
	@Override
	public int sortOrder() {
		return 1_000_000;
	}
}
