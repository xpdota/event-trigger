package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.lang.reflect.Field;
import java.util.List;

public class M8sAltTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m8s_anon_min.log";
	}

	@Override
	protected void modifyCalloutSettings(ModifiedCalloutHandle handle) {
		Field field = handle.getField();
		switch (field == null ? "" : field.getName()) {
			case "moonlightFirst",
			     "moonlightRemaining",
			     "moonlightFirstTwo",
			     "moonlightSecondTwo",
			     "moonlightMoveSecondQuadrant",
			     "moonlightExtraCollFirst" -> handle.getEnable().set(true);
			default -> handle.getEnable().set(false);
		}
	}

	@Override
	protected long minimumMsBetweenCalls() {
		return 0;
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(341939, "Start North", "North"),
				call(343986, "East", "North, East"),
				call(343986, "Start Northeast", "Start Northeast"),
				call(345948, "South", "North, East, South"),
				call(347998, "West", "North, East, South, West"),
				call(347998, "Then Southwest", "Northeast to Southwest"),
				call(347998, "North", "North, East, South, West"),
				call(350894, "East", "East, South, West"),
				call(352945, "South", "South, West"),
				call(352945, "Southwest", "Southwest"),
				call(354906, "West", "West")
		);
	}
}
