package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.models.XivZone;
import org.jetbrains.annotations.Nullable;

public class ZoneIdFilter implements SimpleCondition<Object> {

	@JsonIgnore
	@EditorIgnore
	private final XivState state;

	public NumericOperator operator = NumericOperator.EQ;
	@Description("Zone ID")
	@IdType(ZoneInfo.class)
	public long expected;

	@Description("Use Current Zone")
	@JsonIgnore
	public final Runnable setToCurrent = () -> {
		Long current = getCurrentZoneId();
		if (current != null) {
			expected = current;
		}
	};

	public ZoneIdFilter(@JacksonInject(useInput = OptBoolean.FALSE) XivState state) {
		this.state = state;
	}

	@Override
	public String fixedLabel() {
		return "Zone ID";
	}

	@Override
	public String dynamicLabel() {
		return "Zone ID " + operator.getFriendlyName() + ' ' + expected;
	}

	private @Nullable Long getCurrentZoneId() {
		XivZone zone;
		if (state == null) {
			return null;
		}
		if ((zone = state.getZone()) == null) {
			return null;
		}
		return zone.getId();
	}

	@Override
	public boolean test(Object o) {
		Long currentZone = getCurrentZoneId();
		if (currentZone == null) {
			return false;
		}
		return operator.checkLong(currentZone, expected);
	}
}
