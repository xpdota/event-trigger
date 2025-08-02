package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.xivsupport.events.misc.pulls.Pull;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class PullDurationFilter implements SimpleCondition<Object> {

	private final PullTracker pt;
	@Description("Min Duration")
	public double minDuration = 10.5;
	@Description("Max Duration")
	public double maxDuration = 30.8;
	@Description("Only Consider Time In-Combat")
	public boolean useCombatDuration = true;

	public PullDurationFilter(@JacksonInject(useInput = OptBoolean.FALSE) PullTracker pt) {
		this.pt = pt;
	}


	@JsonIgnore
	private double getCurrentDuration() {
		Pull currentPull = pt.getCurrentPull();
		if (currentPull == null) {
			return 0;
		}
		if (useCombatDuration) {
			Duration cd = currentPull.getCombatDuration();
			if (cd == null) {
				return 0;
			}
			return durationToDouble(cd);
		}
		else {
			return durationToDouble(currentPull.getDuration());
		}
	}

	private static double durationToDouble(Duration duration) {
		return duration.toSeconds() + duration.toMillisPart() * 0.001;
	}

	@Override
	public boolean test(Object event) {
		double cd = getCurrentDuration();
		return minDuration <= cd && cd <= maxDuration;
	}

	@Override
	public @Nullable String fixedLabel() {
		return "Pull Duration";
	}

	@Override
	public String dynamicLabel() {
		return "Pull Duration Between %.1fs and %.1fs".formatted(minDuration, maxDuration);
	}

	@Override
	public Class<Object> getEventType() {
		return Object.class;
	}
}
