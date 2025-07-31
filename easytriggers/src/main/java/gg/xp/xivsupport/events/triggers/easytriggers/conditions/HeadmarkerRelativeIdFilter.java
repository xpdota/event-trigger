package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import org.jetbrains.annotations.Nullable;

public class HeadmarkerRelativeIdFilter implements SimpleCondition<HeadMarkerEvent> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Headmarker ID")
	public long expected;

	@Override
	public @Nullable String fixedLabel() {
		return "Headmarker ID (relative to first HM of the pull)";
	}

	@Override
	public String dynamicLabel() {
		return String.format("Headmarker ID (relative) %s %s%s", operator.getFriendlyName(), expected >= 0 ? "+" : "-", Math.abs(expected));
	}

	@Override
	public boolean test(HeadMarkerEvent event) {
		return operator.checkLong(event.getMarkerOffset(), expected);
	}

	@Override
	public Class<HeadMarkerEvent> getEventType() {
		return HeadMarkerEvent.class;
	}
}
