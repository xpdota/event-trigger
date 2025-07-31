package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import org.jetbrains.annotations.Nullable;

public class HeadmarkerAbsoluteIdFilter implements SimpleCondition<HeadMarkerEvent> {

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Headmarker ID")
	public long expected;

	@Override
	public @Nullable String fixedLabel() {
		return "Headmarker ID";
	}

	@Override
	public String dynamicLabel() {
		return String.format("Headmarker ID %s 0x%x (%s)", operator.getFriendlyName(), expected, expected);
	}

	@Override
	public boolean test(HeadMarkerEvent event) {
		return operator.checkLong(event.getMarkerId(), expected);
	}

	@Override
	public Class<HeadMarkerEvent> getEventType() {
		return HeadMarkerEvent.class;
	}
}
