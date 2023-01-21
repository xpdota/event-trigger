package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import org.jetbrains.annotations.Nullable;

public class TargetabilityChangeFilter implements SimpleCondition<TargetabilityUpdate> {

	@Description("Targetable")
	public boolean targetable = true;

	@Override
	public @Nullable String fixedLabel() {
		return "New Status";
	}

	@Override
	public String dynamicLabel() {
		return "Is Now " + (targetable ? "Targetable" : "Untargetable");
	}

	@Override
	public boolean test(TargetabilityUpdate event) {
		return event.isTargetable();
	}
}
